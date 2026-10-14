package com.aojt.mgmt.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * JWT の設定（{@code app.jwt.*}）。
 *
 * @param issuer 発行者
 * @param accessTokenExpiration アクセストークンの有効期限
 * @param refreshTokenExpiration リフレッシュトークンの有効期限
 * @param privateKeyLocation 秘密鍵（PKCS#8 の PEM）の場所。未設定なら起動時に鍵を自動生成する
 * @param publicKeyLocation 公開鍵（X.509 の PEM）の場所。未設定なら起動時に鍵を自動生成する
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String issuer,
        Duration accessTokenExpiration,
        Duration refreshTokenExpiration,
        Resource privateKeyLocation,
        Resource publicKeyLocation) {

    /**
     * 秘密鍵と公開鍵の両方が設定されているかを返す。
     *
     * @return 両方が存在すれば true
     */
    public boolean hasConfiguredKeys() {
        return privateKeyLocation != null && privateKeyLocation.exists()
                && publicKeyLocation != null && publicKeyLocation.exists();
    }
}
