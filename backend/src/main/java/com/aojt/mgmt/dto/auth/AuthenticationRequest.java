package com.aojt.mgmt.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * ログインリクエスト。
 *
 * @param email メールアドレス
 * @param password パスワード
 */
public record AuthenticationRequest(
        @Schema(example = "taro@example.com") @NotBlank(message = "メールアドレスは必須です") String email,
        @Schema(example = "password123") @NotBlank(message = "パスワードは必須です") String password) {
}
