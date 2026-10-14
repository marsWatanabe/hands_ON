package com.aojt.mgmt.service.jwt;

import com.aojt.mgmt.config.JwtProperties;
import com.aojt.mgmt.entity.User;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_TOKEN_TYPE = "token_type";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final JwtProperties properties;

    public JwtService(JwtEncoder encoder, JwtDecoder decoder, JwtProperties properties) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.properties = properties;
    }

    /** The access token carries the {@code roles} claim. */
    public String generateAccessToken(User user) {
        return encode(user, TYPE_ACCESS, properties.accessTokenExpiration(), List.of(user.getRole().name()));
    }

    /** The refresh token intentionally carries no roles. */
    public String generateRefreshToken(User user) {
        return encode(user, TYPE_REFRESH, properties.refreshTokenExpiration(), null);
    }

    /**
     * Validates signature, expiry and token type of a refresh token.
     *
     * @return the subject (email) of the token
     * @throws JwtException if the token is invalid, expired or not a refresh token
     */
    public String validateRefreshToken(String token) {
        Jwt jwt = decoder.decode(token);
        if (!TYPE_REFRESH.equals(jwt.getClaimAsString(CLAIM_TOKEN_TYPE))) {
            throw new JwtException("Not a refresh token");
        }
        return jwt.getSubject();
    }

    private String encode(User user, String type, Duration validity, List<String> roles) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(user.getEmail())
                .issuedAt(now)
                .expiresAt(now.plus(validity))
                .claim(CLAIM_TOKEN_TYPE, type);
        if (roles != null) {
            claims.claim(CLAIM_ROLES, roles);
        }
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }
}
