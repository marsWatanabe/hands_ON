package com.aojt.mgmt.controller.auth;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aojt.mgmt.config.SecurityConfig;
import com.aojt.mgmt.config.SecurityErrorHandlers;
import com.aojt.mgmt.dto.auth.AuthenticationRequest;
import com.aojt.mgmt.dto.auth.AuthenticationResponse;
import com.aojt.mgmt.dto.auth.RegisterRequest;
import com.aojt.mgmt.dto.user.UserResponse;
import com.aojt.mgmt.exception.ConflictException;
import com.aojt.mgmt.model.Role;
import com.aojt.mgmt.service.auth.AuthenticationService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * {@link AuthenticationController} と {@link TokenRefreshController} のテスト。未認証で呼べること、
 * 入力検証、認証エラー（401）・重複（409）の応答形式、リフレッシュトークンのヘッダ処理を確認する。
 */
@WebMvcTest({AuthenticationController.class, TokenRefreshController.class})
@Import({SecurityConfig.class, SecurityErrorHandlers.class})
class AuthenticationControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AuthenticationService service;

    @MockitoBean
    JwtDecoder jwtDecoder;

    private static final AuthenticationResponse TOKENS = new AuthenticationResponse("access", "refresh",
            new UserResponse(1, "太郎", "山田", "taro@example.com", Role.USER));

    private ResultActions postJson(String url, String json) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String registerJson(String firstname, String lastname, String email, String password) {
        return """
                {"firstname":"%s","lastname":"%s","email":"%s","password":"%s"}
                """.formatted(firstname, lastname, email, password);
    }

    private static Jwt refreshJwt() {
        return Jwt.withTokenValue("refresh-token").header("alg", "RS256").subject("taro@example.com")
                .claim("token_type", "refresh").issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                .build();
    }

    // ---- register ----

    /** 認証なしで登録でき、トークンとユーザー情報（パスワードを含まない）を返すこと。 */
    @Test
    void register_returnsTokensWithoutAuthentication() throws Exception {
        when(service.register(any(RegisterRequest.class))).thenReturn(TOKENS);

        postJson("/api/v1/auth/register", registerJson("太郎", "山田", "taro@example.com", "password123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.refreshToken").value("refresh"))
                .andExpect(jsonPath("$.user.email").value("taro@example.com"))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    /** 不正な入力（空、形式、桁数）は、サービスを呼ばずに 400 にすること（境界値を含む）。 */
    @Test
    void register_rejectsInvalidInput() throws Exception {
        String[][] invalid = {
                {"", "山田", "taro@example.com", "password123", "firstname"},
                {"太郎", "", "taro@example.com", "password123", "lastname"},
                {"太郎", "山田", "not-an-email", "password123", "email"},
                {"太郎", "山田", "", "password123", "email"},
                {"太郎", "山田", "taro@example.com", "short12", "password"},
                {"太郎", "山田", "taro@example.com", "a".repeat(73), "password"},
                {"あ".repeat(101), "山田", "taro@example.com", "password123", "firstname"},
                {"<b>", "山田", "taro@example.com", "password123", "firstname"}
        };
        for (String[] v : invalid) {
            postJson("/api/v1/auth/register", registerJson(v[0], v[1], v[2], v[3]))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.error.message", containsString(v[4])));
        }
        verifyNoInteractions(service);
    }

    /** パスワードが 8 文字・72 文字ちょうど（境界値）なら受け付けること。 */
    @Test
    void register_acceptsPasswordBoundaryLengths() throws Exception {
        when(service.register(any(RegisterRequest.class))).thenReturn(TOKENS);

        postJson("/api/v1/auth/register", registerJson("太郎", "山田", "taro@example.com", "a".repeat(8)))
                .andExpect(status().isOk());
        postJson("/api/v1/auth/register", registerJson("太郎", "山田", "taro@example.com", "a".repeat(72)))
                .andExpect(status().isOk());
    }

    /** 登録済みのメールアドレスは、409（CONFLICT）にすること。 */
    @Test
    void register_mapsDuplicateEmailTo409() throws Exception {
        when(service.register(any(RegisterRequest.class))).thenThrow(new ConflictException("登録済みです。"));

        postJson("/api/v1/auth/register", registerJson("太郎", "山田", "taro@example.com", "password123"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    // ---- authenticate ----

    /** 認証なしでログインでき、トークンを返すこと。 */
    @Test
    void authenticate_returnsTokens() throws Exception {
        when(service.authenticate(any(AuthenticationRequest.class))).thenReturn(TOKENS);

        postJson("/api/v1/auth/authenticate", "{\"email\":\"taro@example.com\",\"password\":\"password123\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"));
    }

    /** 認証情報が誤っていれば、401（AUTH_ERROR）にすること。 */
    @Test
    void authenticate_mapsBadCredentialsTo401() throws Exception {
        when(service.authenticate(any(AuthenticationRequest.class)))
                .thenThrow(new BadCredentialsException("メールアドレスまたはパスワードが正しくありません。"));

        postJson("/api/v1/auth/authenticate", "{\"email\":\"taro@example.com\",\"password\":\"wrong\"}")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_ERROR"))
                .andExpect(jsonPath("$.error.message").value("メールアドレスまたはパスワードが正しくありません。"));
    }

    /** メールアドレス・パスワードが空なら、400 にすること。 */
    @Test
    void authenticate_rejectsBlankFields() throws Exception {
        postJson("/api/v1/auth/authenticate", "{\"email\":\"\",\"password\":\"\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(service);
    }

    // ---- refresh-token ----

    /** Authorization ヘッダの Bearer トークンをサービスに渡し、新しいトークンを返すこと。 */
    @Test
    void refreshToken_passesBearerTokenToService() throws Exception {
        when(jwtDecoder.decode("refresh-token")).thenReturn(refreshJwt());
        when(service.refresh("refresh-token")).thenReturn(new AuthenticationResponse("new-a", "new-r", null));

        mvc.perform(post("/api/v1/auth/refresh-token").header(HttpHeaders.AUTHORIZATION, "Bearer refresh-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-a"))
                .andExpect(jsonPath("$.user").doesNotExist());

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(service).refresh(captor.capture());
        assertEquals("refresh-token", captor.getValue());
    }

    /** ヘッダが無い、または Bearer 形式でなければ、401（AUTH_ERROR）にすること。 */
    @Test
    void refreshToken_returns401WhenHeaderIsMissingOrNotBearer() throws Exception {
        mvc.perform(post("/api/v1/auth/refresh-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_ERROR"));
        mvc.perform(post("/api/v1/auth/refresh-token").header(HttpHeaders.AUTHORIZATION, "Basic abc"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_ERROR"));
        verifyNoInteractions(service);
    }

    /** トークンが空の Bearer ヘッダは、401 にすること。 */
    @Test
    void refreshToken_returns401ForEmptyBearerToken() throws Exception {
        mvc.perform(post("/api/v1/auth/refresh-token").header(HttpHeaders.AUTHORIZATION, "Bearer "))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    /** サービスが無効なトークンと判定したら、401（AUTH_ERROR）にすること。 */
    @Test
    void refreshToken_mapsInvalidTokenTo401() throws Exception {
        when(jwtDecoder.decode("refresh-token")).thenReturn(refreshJwt());
        when(service.refresh("refresh-token")).thenThrow(new BadCredentialsException("リフレッシュトークンが無効です。"));

        mvc.perform(post("/api/v1/auth/refresh-token").header(HttpHeaders.AUTHORIZATION, "Bearer refresh-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_ERROR"));
    }
}
