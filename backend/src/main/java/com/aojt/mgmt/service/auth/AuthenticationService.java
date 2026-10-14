package com.aojt.mgmt.service.auth;

import com.aojt.mgmt.dto.auth.AuthenticationRequest;
import com.aojt.mgmt.dto.auth.AuthenticationResponse;
import com.aojt.mgmt.dto.auth.RegisterRequest;
import com.aojt.mgmt.dto.user.UserResponse;
import com.aojt.mgmt.exception.ConflictException;
import com.aojt.mgmt.model.Role;
import com.aojt.mgmt.model.User;
import com.aojt.mgmt.repository.user.UserRepository;
import com.aojt.mgmt.service.jwt.JwtService;
import java.util.Locale;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 認証の業務ロジック。ユーザー登録、ログイン、トークン更新を行う。
 */
@Service
public class AuthenticationService {

    /** ユーザーの存在有無を推測されないよう、メールとパスワードのどちらが誤りかは区別しない。 */
    private static final String MSG_BAD_CREDENTIALS = "メールアドレスまたはパスワードが正しくありません。";
    private static final String MSG_INVALID_REFRESH_TOKEN = "リフレッシュトークンが無効です。";
    private static final String MSG_EMAIL_EXISTS = "このメールアドレスは既に登録されています。";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * サービスを生成する。
     *
     * @param userRepository ユーザーのリポジトリ
     * @param passwordEncoder パスワードのエンコーダー
     * @param jwtService JWT サービス
     */
    public AuthenticationService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * ユーザーを登録し、トークンを発行する。ロールは {@link Role#USER}。メールアドレスは小文字に統一する。
     *
     * @param request 登録内容
     * @return トークンとユーザー情報
     * @throws ConflictException メールアドレスが登録済みの場合
     */
    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException(MSG_EMAIL_EXISTS);
        }
        User user = userRepository.save(new User(
                request.firstname(), request.lastname(), email,
                passwordEncoder.encode(request.password()), Role.USER));
        return tokens(user);
    }

    /**
     * ログインし、トークンを発行する。
     *
     * @param request メールアドレスとパスワード
     * @return トークンとユーザー情報
     * @throws BadCredentialsException メールアドレスまたはパスワードが正しくない場合
     */
    @Transactional(readOnly = true)
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        User user = userRepository.findByEmail(normalize(request.email()))
                .orElseThrow(() -> new BadCredentialsException(MSG_BAD_CREDENTIALS));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException(MSG_BAD_CREDENTIALS);
        }
        return tokens(user);
    }

    /**
     * リフレッシュトークンから、新しいトークンを発行する。
     *
     * @param refreshToken リフレッシュトークン
     * @return 新しいアクセストークンとリフレッシュトークン（ユーザー情報は含まない）
     * @throws BadCredentialsException トークンが無効、またはユーザーが存在しない場合
     */
    @Transactional(readOnly = true)
    public AuthenticationResponse refresh(String refreshToken) {
        String email;
        try {
            email = jwtService.validateRefreshToken(refreshToken);
        } catch (JwtException e) {
            throw new BadCredentialsException(MSG_INVALID_REFRESH_TOKEN);
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException(MSG_INVALID_REFRESH_TOKEN));
        return new AuthenticationResponse(
                jwtService.generateAccessToken(user), jwtService.generateRefreshToken(user), null);
    }

    private AuthenticationResponse tokens(User user) {
        return new AuthenticationResponse(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                UserResponse.from(user));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
