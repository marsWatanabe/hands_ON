package com.aojt.mgmt.controller.auth;

import com.aojt.mgmt.dto.auth.AuthenticationRequest;
import com.aojt.mgmt.dto.auth.AuthenticationResponse;
import com.aojt.mgmt.dto.auth.RegisterRequest;
import com.aojt.mgmt.service.auth.AuthenticationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 認証 API（ユーザー登録、ログイン）。{@code /api/v1/auth/**} は未認証でも呼べる。
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "認証関連 API")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    /**
     * コントローラーを生成する。
     *
     * @param authenticationService 認証サービス
     */
    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * ユーザーを登録し、トークンを返す。
     *
     * @param request 登録内容
     * @return トークンとユーザー情報
     */
    @PostMapping("/register")
    public AuthenticationResponse register(@Valid @RequestBody RegisterRequest request) {
        return authenticationService.register(request);
    }

    /**
     * ログインし、トークンを返す。
     *
     * @param request メールアドレスとパスワード
     * @return トークンとユーザー情報
     */
    @PostMapping("/authenticate")
    public AuthenticationResponse authenticate(@Valid @RequestBody AuthenticationRequest request) {
        return authenticationService.authenticate(request);
    }
}
