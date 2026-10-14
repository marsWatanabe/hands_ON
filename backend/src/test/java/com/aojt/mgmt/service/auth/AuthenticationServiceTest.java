package com.aojt.mgmt.service.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aojt.mgmt.dto.auth.AuthenticationRequest;
import com.aojt.mgmt.dto.auth.AuthenticationResponse;
import com.aojt.mgmt.dto.auth.RegisterRequest;
import com.aojt.mgmt.exception.ConflictException;
import com.aojt.mgmt.model.Role;
import com.aojt.mgmt.model.User;
import com.aojt.mgmt.repository.user.UserRepository;
import com.aojt.mgmt.service.jwt.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * {@link AuthenticationService} の単体テスト。ユーザー登録、ログイン、トークン更新の正常系・異常系を確認する。
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtService jwtService;

    @InjectMocks
    AuthenticationService service;

    private static User user(String email) {
        return new User("太郎", "山田", email, "hash", Role.USER);
    }

    // ---- register ----

    /** メールアドレスを小文字・前後空白なしにし、パスワードをハッシュ化して USER で保存し、トークンを返すこと。 */
    @Test
    void register_savesNormalizedUserWithHashedPasswordAndReturnsTokens() {
        when(userRepository.existsByEmail("taro@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh");

        AuthenticationResponse response = service.register(
                new RegisterRequest("太郎", "山田", "  Taro@Example.COM ", "password123"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("taro@example.com", captor.getValue().getEmail());
        assertEquals("encoded", captor.getValue().getPassword());
        assertEquals(Role.USER, captor.getValue().getRole());
        assertEquals("access", response.accessToken());
        assertEquals("refresh", response.refreshToken());
        assertEquals("taro@example.com", response.user().email());
    }

    /** 登録済みのメールアドレスは、保存せずに 409 用の例外にすること。 */
    @Test
    void register_throwsConflictWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("taro@example.com")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> service.register(new RegisterRequest("太郎", "山田", "taro@example.com", "password123")));
        verify(userRepository, never()).save(any(User.class));
    }

    // ---- authenticate ----

    /** メールアドレスとパスワードが正しければ、トークンとユーザー情報（パスワードを含まない）を返すこと。 */
    @Test
    void authenticate_returnsTokensForValidCredentials() {
        User user = user("taro@example.com");
        when(userRepository.findByEmail("taro@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hash")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh");

        AuthenticationResponse response = service.authenticate(
                new AuthenticationRequest(" Taro@example.com ", "password123"));

        assertEquals("access", response.accessToken());
        assertEquals("refresh", response.refreshToken());
        assertEquals(Role.USER, response.user().role());
    }

    /** パスワードが違えば認証エラーにすること。 */
    @Test
    void authenticate_throwsBadCredentialsForWrongPassword() {
        when(userRepository.findByEmail("taro@example.com")).thenReturn(Optional.of(user("taro@example.com")));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThrows(BadCredentialsException.class,
                () -> service.authenticate(new AuthenticationRequest("taro@example.com", "wrong")));
    }

    /** 存在しないメールアドレスも、パスワード誤りと同じ認証エラー（同じメッセージ）にすること。 */
    @Test
    void authenticate_usesTheSameErrorForUnknownEmailAndWrongPassword() {
        when(userRepository.findByEmail("taro@example.com")).thenReturn(Optional.of(user("taro@example.com")));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);
        when(userRepository.findByEmail("none@example.com")).thenReturn(Optional.empty());

        BadCredentialsException wrongPassword = assertThrows(BadCredentialsException.class,
                () -> service.authenticate(new AuthenticationRequest("taro@example.com", "wrong")));
        BadCredentialsException unknownEmail = assertThrows(BadCredentialsException.class,
                () -> service.authenticate(new AuthenticationRequest("none@example.com", "wrong")));

        assertEquals(wrongPassword.getMessage(), unknownEmail.getMessage());
    }

    // ---- refresh ----

    /** 有効なリフレッシュトークンから、ユーザー情報を含まない新しいトークンを発行すること。 */
    @Test
    void refresh_issuesNewTokensWithoutUser() {
        User user = user("taro@example.com");
        when(jwtService.validateRefreshToken("old")).thenReturn("taro@example.com");
        when(userRepository.findByEmail("taro@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("new-access");
        when(jwtService.generateRefreshToken(user)).thenReturn("new-refresh");

        AuthenticationResponse response = service.refresh("old");

        assertEquals("new-access", response.accessToken());
        assertEquals("new-refresh", response.refreshToken());
        assertNull(response.user());
    }

    /** トークンの検証に失敗したら、認証エラーにすること。 */
    @Test
    void refresh_throwsBadCredentialsWhenTokenIsInvalid() {
        when(jwtService.validateRefreshToken("bad")).thenThrow(new JwtException("invalid"));

        assertThrows(BadCredentialsException.class, () -> service.refresh("bad"));
    }

    /** トークンは有効でも、ユーザーが存在しなければ認証エラーにすること。 */
    @Test
    void refresh_throwsBadCredentialsWhenUserNoLongerExists() {
        when(jwtService.validateRefreshToken("old")).thenReturn("gone@example.com");
        when(userRepository.findByEmail("gone@example.com")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> service.refresh("old"));
    }
}
