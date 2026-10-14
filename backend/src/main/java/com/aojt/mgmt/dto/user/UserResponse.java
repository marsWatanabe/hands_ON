package com.aojt.mgmt.dto.user;

import com.aojt.mgmt.model.Role;
import com.aojt.mgmt.model.User;

/**
 * ユーザー情報のレスポンス。パスワード（ハッシュ）は含めない。
 *
 * @param id ID
 * @param firstname 名
 * @param lastname 姓
 * @param email メールアドレス
 * @param role ロール
 */
public record UserResponse(Integer id, String firstname, String lastname, String email, Role role) {

    /**
     * エンティティからレスポンスを生成する。
     *
     * @param user ユーザー
     * @return レスポンス
     */
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(), user.getFirstname(), user.getLastname(), user.getEmail(), user.getRole());
    }
}
