package com.aojt.mgmt.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * ユーザー登録リクエスト。文字数の上限は {@code _user} テーブルの桁数に合わせる。
 *
 * @param firstname 名
 * @param lastname 姓
 * @param email メールアドレス
 * @param password パスワード（8〜72文字）
 */
public record RegisterRequest(
        @Schema(example = "Taro") @NotBlank(message = "名は必須です") @Size(max = 100, message = "名は100文字以内で入力してください")
        @Pattern(regexp = "^[^<>]*$", message = "名に < または > は使用できません")
        String firstname,

        @Schema(example = "Yamada") @NotBlank(message = "姓は必須です") @Size(max = 100, message = "姓は100文字以内で入力してください")
        @Pattern(regexp = "^[^<>]*$", message = "姓に < または > は使用できません")
        String lastname,

        @Schema(example = "taro@example.com") @NotBlank(message = "メールアドレスは必須です")
        @Email(message = "メールアドレスの形式が不正です") @Size(max = 255, message = "メールアドレスは255文字以内で入力してください")
        String email,

        @Schema(example = "password123", description = "8〜72文字") @NotBlank(message = "パスワードは必須です")
        @Size(min = 8, max = 72, message = "パスワードは8〜72文字で入力してください")
        String password) {
}
