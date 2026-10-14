package com.aojt.mgmt.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;

/**
 * {@link JwtConfig} と {@link JwtProperties} のテスト。鍵の自動生成、PEM ファイルからの読み込み、
 * エンコーダーとデコーダーの往復を確認する。
 */
class JwtConfigTest {

    private final JwtConfig config = new JwtConfig();

    private static JwtProperties properties(Path privateKey, Path publicKey) {
        return new JwtProperties("test", Duration.ofMinutes(15), Duration.ofDays(7),
                privateKey == null ? null : new FileSystemResource(privateKey),
                publicKey == null ? null : new FileSystemResource(publicKey));
    }

    private static String pem(String type, byte[] der) {
        return "-----BEGIN " + type + "-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(der)
                + "\n-----END " + type + "-----\n";
    }

    private static String roundTrip(KeyPair keyPair, JwtConfig config) {
        JwtEncoder encoder = config.jwtEncoder(keyPair);
        JwtDecoder decoder = config.jwtDecoder(keyPair);
        JwtClaimsSet claims = JwtClaimsSet.builder().subject("taro@example.com")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        Jwt decoded = decoder.decode(encoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims)).getTokenValue());
        return decoded.getSubject();
    }

    /** 鍵が設定されていなければ、2048 ビットの RSA 鍵ペアを自動生成すること。 */
    @Test
    void jwtKeyPair_generatesKeyPairWhenNoKeysAreConfigured() throws Exception {
        KeyPair keyPair = config.jwtKeyPair(properties(null, null));

        assertEquals("RSA", keyPair.getPublic().getAlgorithm());
        assertNotNull(keyPair.getPrivate());
    }

    /** 自動生成の鍵は、起動ごとに異なること（開発用）。 */
    @Test
    void jwtKeyPair_generatesDifferentKeysEachTime() throws Exception {
        KeyPair first = config.jwtKeyPair(properties(null, null));
        KeyPair second = config.jwtKeyPair(properties(null, null));

        assertFalse(java.util.Arrays.equals(first.getPublic().getEncoded(), second.getPublic().getEncoded()));
    }

    /** PEM ファイル（PKCS#8 の秘密鍵、X.509 の公開鍵）から鍵を読み込めること。 */
    @Test
    void jwtKeyPair_loadsKeysFromPemFiles(@TempDir Path dir) throws Exception {
        KeyPair generated = config.jwtKeyPair(properties(null, null));
        Path privateFile = Files.writeString(dir.resolve("private.pem"),
                pem("PRIVATE KEY", generated.getPrivate().getEncoded()));
        Path publicFile = Files.writeString(dir.resolve("public.pem"),
                pem("PUBLIC KEY", generated.getPublic().getEncoded()));

        KeyPair loaded = config.jwtKeyPair(properties(privateFile, publicFile));

        assertArrayEquals(generated.getPublic().getEncoded(), loaded.getPublic().getEncoded());
        assertArrayEquals(generated.getPrivate().getEncoded(), loaded.getPrivate().getEncoded());
    }

    /** 片方のファイルしか存在しない場合は、設定済みとは見なさないこと。 */
    @Test
    void hasConfiguredKeys_requiresBothFiles(@TempDir Path dir) throws Exception {
        Path existing = Files.writeString(dir.resolve("a.pem"), "x");

        assertFalse(properties(null, null).hasConfiguredKeys());
        assertFalse(properties(existing, null).hasConfiguredKeys());
        assertFalse(properties(existing, dir.resolve("missing.pem")).hasConfiguredKeys());
        assertTrue(properties(existing, existing).hasConfiguredKeys());
    }

    /** エンコーダーで署名したトークンを、デコーダーで検証して読み出せること。 */
    @Test
    void encoderAndDecoder_roundTrip() throws Exception {
        KeyPair keyPair = config.jwtKeyPair(properties(null, null));

        assertEquals("taro@example.com", roundTrip(keyPair, config));
    }

    /** 別の鍵ペアのデコーダーでは、検証に失敗すること。 */
    @Test
    void decoder_rejectsTokenSignedByAnotherKey() throws Exception {
        KeyPair signer = config.jwtKeyPair(properties(null, null));
        KeyPair other = config.jwtKeyPair(properties(null, null));
        JwtClaimsSet claims = JwtClaimsSet.builder().subject("x")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        String token = config.jwtEncoder(signer).encode(
                JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims)).getTokenValue();

        assertThrows(Exception.class, () -> config.jwtDecoder(other).decode(token));
    }
}
