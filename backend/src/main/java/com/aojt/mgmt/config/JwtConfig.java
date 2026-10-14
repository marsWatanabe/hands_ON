package com.aojt.mgmt.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * JWT の RSA 鍵と、エンコーダー・デコーダー（RS256）の設定。
 */
@Configuration
public class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);

    private static final String KEY_ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048;
    private static final String KEY_ID = "inventrack-key";

    /**
     * RSA 鍵ペアを用意する。PEM ファイルが設定されていれば読み込み、無ければ起動ごとに自動生成する（開発用）。
     *
     * @param properties JWT の設定
     * @return 鍵ペア
     * @throws GeneralSecurityException 鍵の生成・復元に失敗した場合
     * @throws IOException 鍵ファイルの読み込みに失敗した場合
     */
    @Bean
    KeyPair jwtKeyPair(JwtProperties properties) throws GeneralSecurityException, IOException {
        if (properties.hasConfiguredKeys()) {
            KeyFactory factory = KeyFactory.getInstance(KEY_ALGORITHM);
            RSAPrivateKey privateKey = (RSAPrivateKey) factory.generatePrivate(
                    new PKCS8EncodedKeySpec(readPem(properties.privateKeyLocation())));
            RSAPublicKey publicKey = (RSAPublicKey) factory.generatePublic(
                    new X509EncodedKeySpec(readPem(properties.publicKeyLocation())));
            return new KeyPair(publicKey, privateKey);
        }
        log.warn("JWT の鍵が設定されていないため、起動ごとに RSA 鍵ペアを自動生成します（開発用）。");
        KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
        generator.initialize(KEY_SIZE);
        return generator.generateKeyPair();
    }

    /**
     * トークン生成用のエンコーダー。
     *
     * @param keyPair 鍵ペア
     * @return JWT エンコーダー
     */
    @Bean
    JwtEncoder jwtEncoder(KeyPair keyPair) {
        RSAKey jwk = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey(keyPair.getPrivate())
                .keyID(KEY_ID)
                .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
    }

    /**
     * トークン検証用のデコーダー（公開鍵で署名を検証する）。
     *
     * @param keyPair 鍵ペア
     * @return JWT デコーダー
     */
    @Bean
    JwtDecoder jwtDecoder(KeyPair keyPair) {
        return NimbusJwtDecoder.withPublicKey((RSAPublicKey) keyPair.getPublic()).build();
    }

    private static byte[] readPem(Resource resource) throws IOException {
        try (InputStream in = resource.getInputStream()) {
            String pem = new String(in.readAllBytes(), StandardCharsets.US_ASCII)
                    .replaceAll("-----(BEGIN|END) [A-Z ]+-----", "")
                    .replaceAll("\\s", "");
            return Base64.getDecoder().decode(pem);
        }
    }
}
