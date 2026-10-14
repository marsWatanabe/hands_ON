package com.aojt.mgmt.service.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.aojt.mgmt.config.JwtProperties;
import com.aojt.mgmt.model.Role;
import com.aojt.mgmt.model.User;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * {@link JwtService} の単体テスト。実際の RSA 鍵で発行・検証し、クレーム、種類の区別、期限切れ、改ざんを確認する。
 */
class JwtServiceTest {

    private static final String ISSUER = "inventrack-test";

    private KeyPair keyPair;
    private JwtDecoder decoder;
    private JwtService service;
    private final User user = new User("太郎", "山田", "taro@example.com", "hash", Role.ADMIN);

    private static KeyPair newKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static JwtEncoder encoder(KeyPair keyPair) {
        RSAKey jwk = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey(keyPair.getPrivate()).build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
    }

    private static JwtDecoder decoder(KeyPair keyPair) {
        return NimbusJwtDecoder.withPublicKey((RSAPublicKey) keyPair.getPublic()).build();
    }

    private JwtService serviceWith(Duration access, Duration refresh) {
        return new JwtService(encoder(keyPair), decoder, new JwtProperties(ISSUER, access, refresh, null, null));
    }

    @BeforeEach
    void setUp() throws Exception {
        keyPair = newKeyPair();
        decoder = decoder(keyPair);
        service = serviceWith(Duration.ofMinutes(15), Duration.ofDays(7));
    }

    /** アクセストークンが、発行者、subject（メール）、ロール、種類 access、有効期限を含むこと。 */
    @Test
    void generateAccessToken_containsSubjectRolesTypeAndExpiry() {
        Jwt jwt = decoder.decode(service.generateAccessToken(user));

        assertEquals(ISSUER, jwt.getClaimAsString("iss"));
        assertEquals("taro@example.com", jwt.getSubject());
        assertEquals(List.of("ADMIN"), jwt.getClaimAsStringList(JwtService.CLAIM_ROLES));
        assertEquals(JwtService.TYPE_ACCESS, jwt.getClaimAsString(JwtService.CLAIM_TOKEN_TYPE));
        assertEquals(Duration.ofMinutes(15), Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()));
    }

    /** リフレッシュトークンが、ロールを含まず、種類 refresh で、設定した有効期限を持つこと。 */
    @Test
    void generateRefreshToken_hasNoRolesAndIsTypedRefresh() {
        Jwt jwt = decoder.decode(service.generateRefreshToken(user));

        assertFalse(jwt.hasClaim(JwtService.CLAIM_ROLES));
        assertEquals(JwtService.TYPE_REFRESH, jwt.getClaimAsString(JwtService.CLAIM_TOKEN_TYPE));
        assertEquals(Duration.ofDays(7), Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()));
    }

    /** 有効なリフレッシュトークンから subject を取り出せること。 */
    @Test
    void validateRefreshToken_returnsSubjectOfValidToken() {
        assertEquals("taro@example.com", service.validateRefreshToken(service.generateRefreshToken(user)));
    }

    /** アクセストークンはリフレッシュトークンとして受け付けないこと。 */
    @Test
    void validateRefreshToken_rejectsAccessToken() {
        assertThrows(JwtException.class, () -> service.validateRefreshToken(service.generateAccessToken(user)));
    }

    /** 期限切れのトークンは受け付けないこと。 */
    @Test
    void validateRefreshToken_rejectsExpiredToken() {
        Instant issued = Instant.now().minus(Duration.ofMinutes(10));
        JwtClaimsSet claims = JwtClaimsSet.builder().subject(user.getEmail())
                .issuedAt(issued).expiresAt(issued.plus(Duration.ofMinutes(5)))
                .claim(JwtService.CLAIM_TOKEN_TYPE, JwtService.TYPE_REFRESH).build();
        String expired = encoder(keyPair).encode(
                JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims)).getTokenValue();

        assertThrows(JwtException.class, () -> service.validateRefreshToken(expired));
    }

    /** 別の鍵で署名されたトークンは受け付けないこと。 */
    @Test
    void validateRefreshToken_rejectsTokenSignedWithAnotherKey() throws Exception {
        JwtService other = new JwtService(encoder(newKeyPair()), decoder,
                new JwtProperties(ISSUER, Duration.ofMinutes(15), Duration.ofDays(7), null, null));

        assertThrows(JwtException.class, () -> service.validateRefreshToken(other.generateRefreshToken(user)));
    }

    /** 署名を改ざんしたトークンは受け付けないこと。 */
    @Test
    void validateRefreshToken_rejectsTamperedToken() {
        String token = service.generateRefreshToken(user);
        String tampered = token.substring(0, token.length() - 4)
                + (token.endsWith("AAAA") ? "BBBB" : "AAAA");

        assertThrows(JwtException.class, () -> service.validateRefreshToken(tampered));
    }

    /** JWT の形式ではない文字列は受け付けないこと。 */
    @Test
    void validateRefreshToken_rejectsMalformedToken() {
        assertThrows(JwtException.class, () -> service.validateRefreshToken("not-a-jwt"));
    }
}
