package com.aojt.mgmt.controller.user;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aojt.mgmt.config.SecurityConfig;
import com.aojt.mgmt.config.SecurityErrorHandlers;
import com.aojt.mgmt.model.Role;
import com.aojt.mgmt.model.User;
import com.aojt.mgmt.repository.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * {@link UserController} のテスト。パスワードを返さないこと、本人または管理者だけが参照できること、
 * 未認証・存在しないユーザーの応答を確認する。
 */
@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, SecurityErrorHandlers.class})
class UserControllerTest {

    private static final String SELF = "taro@example.com";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    UserRepository userRepository;

    @MockitoBean
    JwtDecoder jwtDecoder;

    private void givenUser(int id, String email) {
        when(userRepository.findById(id)).thenReturn(Optional.of(new User("太郎", "山田", email, "secret-hash", Role.USER)));
    }

    /** トークンが無ければ、401（AUTH_ERROR）にすること。 */
    @Test
    void getUser_returns401WithoutAuthentication() throws Exception {
        mvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_ERROR"));
    }

    /** 本人の情報を、パスワードを含まない形で返すこと。 */
    @Test
    @WithMockUser(username = SELF, roles = "USER")
    void getUser_returnsOwnUserWithoutPassword() throws Exception {
        givenUser(1, SELF);

        mvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(SELF))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    /** 管理者以外が他のユーザーを参照すると、403（FORBIDDEN）にすること。 */
    @Test
    @WithMockUser(username = SELF, roles = "USER")
    void getUser_returns403WhenReadingAnotherUser() throws Exception {
        givenUser(2, "other@example.com");

        mvc.perform(get("/api/v1/users/2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    /** 管理者は他のユーザーも参照できること。 */
    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void getUser_allowsAdminToReadAnyUser() throws Exception {
        givenUser(2, "other@example.com");

        mvc.perform(get("/api/v1/users/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("other@example.com"));
    }

    /** 存在しないユーザーは、404（NOT_FOUND）にすること。 */
    @Test
    @WithMockUser(username = SELF, roles = "USER")
    void getUser_returns404WhenUserDoesNotExist() throws Exception {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }
}
