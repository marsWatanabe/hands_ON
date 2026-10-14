package com.aojt.mgmt.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * アプリケーションユーザー（テーブル {@code _user}）。
 *
 * <p>スキーマは Flyway（{@code db/migration}）が管理し、本クラスは起動時に構造を検証される。
 */
@Entity
@Table(name = "_user")
public class User {

    /** ID（自動採番）。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** 名。 */
    @Column(nullable = false, length = 100)
    private String firstname;

    /** 姓。 */
    @Column(nullable = false, length = 100)
    private String lastname;

    /** メールアドレス（ログインに使用。ユニーク）。 */
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /** パスワードの BCrypt ハッシュ。 */
    @Column(nullable = false)
    private String password;

    /** ロール。 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Role role;

    /** JPA 用の引数なしコンストラクタ。 */
    protected User() {
    }

    /**
     * ユーザーを生成する。
     *
     * @param firstname 名
     * @param lastname 姓
     * @param email メールアドレス
     * @param password パスワードのハッシュ
     * @param role ロール
     */
    public User(String firstname, String lastname, String email, String password, Role role) {
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    /**
     * IDを返す。
     *
     * @return ID
     */
    public Integer getId() {
        return id;
    }

    /**
     * 名を返す。
     *
     * @return 名
     */
    public String getFirstname() {
        return firstname;
    }

    /**
     * 姓を返す。
     *
     * @return 姓
     */
    public String getLastname() {
        return lastname;
    }

    /**
     * メールアドレスを返す。
     *
     * @return メールアドレス
     */
    public String getEmail() {
        return email;
    }

    /**
     * パスワードのハッシュを返す。
     *
     * @return パスワードのハッシュ
     */
    public String getPassword() {
        return password;
    }

    /**
     * ロールを返す。
     *
     * @return ロール
     */
    public Role getRole() {
        return role;
    }
}
