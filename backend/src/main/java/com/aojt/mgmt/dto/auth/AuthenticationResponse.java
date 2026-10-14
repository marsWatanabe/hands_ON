package com.aojt.mgmt.dto.auth;

import com.aojt.mgmt.dto.user.UserResponse;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 認証レスポンス。トークン更新のときは {@code user} を含まない（null は出力されない）。
 *
 * @param accessToken アクセストークン
 * @param refreshToken リフレッシュトークン
 * @param user ユーザー情報（パスワードを含まない）
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthenticationResponse(String accessToken, String refreshToken, UserResponse user) {
}
