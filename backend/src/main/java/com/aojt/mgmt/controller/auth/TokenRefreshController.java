package com.aojt.mgmt.controller.auth;

import com.aojt.mgmt.dto.auth.AuthenticationResponse;
import com.aojt.mgmt.service.auth.AuthenticationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * トークン更新 API。リフレッシュトークンを {@code Authorization: Bearer} ヘッダで受け取る。
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "認証関連 API")
public class TokenRefreshController {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String MSG_BEARER_REQUIRED = "Bearer 形式のリフレッシュトークンが必要です。";

    private final AuthenticationService authenticationService;

    /**
     * コントローラーを生成する。
     *
     * @param authenticationService 認証サービス
     */
    public TokenRefreshController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * リフレッシュトークンから、新しいトークンを返す。
     *
     * @param authorization {@code Authorization} ヘッダ（{@code Bearer <refreshToken>}）
     * @return 新しいアクセストークンとリフレッシュトークン
     * @throws BadCredentialsException ヘッダが無い、または形式が不正な場合（401）
     */
    @PostMapping("/refresh-token")
    public AuthenticationResponse refreshToken(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)
                || authorization.length() == BEARER_PREFIX.length()) {
            throw new BadCredentialsException(MSG_BEARER_REQUIRED);
        }
        return authenticationService.refresh(authorization.substring(BEARER_PREFIX.length()).trim());
    }
}
