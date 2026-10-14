package com.aojt.mgmt.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String issuer,
        Duration accessTokenExpiration,
        Duration refreshTokenExpiration,
        Resource privateKeyLocation,
        Resource publicKeyLocation) {

    public boolean hasConfiguredKeys() {
        return privateKeyLocation != null && privateKeyLocation.exists()
                && publicKeyLocation != null && publicKeyLocation.exists();
    }
}
