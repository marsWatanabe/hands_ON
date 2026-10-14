package com.aojt.mgmt.repository.user;

import com.aojt.mgmt.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * ユーザーのリポジトリ。
 */
public interface UserRepository extends JpaRepository<User, Integer> {

    /**
     * メールアドレスでユーザーを検索する。
     *
     * @param email メールアドレス
     * @return 該当するユーザー
     */
    Optional<User> findByEmail(String email);

    /**
     * メールアドレスが登録済みかを確認する。
     *
     * @param email メールアドレス
     * @return 登録済みなら true
     */
    boolean existsByEmail(String email);
}
