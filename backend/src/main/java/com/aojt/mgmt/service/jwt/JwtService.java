package com.aojt.mgmt.service.jwt;

import com.aojt.mgmt.config.JwtProperties;
import com.aojt.mgmt.model.User;
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

/**
 * JWT（アクセストークン、リフレッシュトークン）の発行と検証。署名は RS256。
 */
@Service
public class JwtService {

    /** ロールのクレーム名。 */
    public static final String CLAIM_ROLES = "roles";

    /** トークンの種類のクレーム名。 */
    public static final String CLAIM_TOKEN_TYPE = "token_type";

    /** トークンの種類: アクセストークン。 */
    public static final String TYPE_ACCESS = "access";

    /** トークンの種類: リフレッシュトークン。 */
    public static final String TYPE_REFRESH = "refresh";

    private static final String MSG_NOT_REFRESH_TOKEN = "リフレッシュトークンではありません";

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final JwtProperties properties;

    /**
     * サービスを生成する。
     *
     * @param encoder JWT のエンコーダー
     * @param decoder JWT のデコーダー
     * @param properties JWT の設定（発行者、有効期限）
     */
    public JwtService(JwtEncoder encoder, JwtDecoder decoder, JwtProperties properties) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.properties = properties;
    }

    /**
     * アクセストークンを発行する。{@code roles} クレームにロールを含む。
     *
     * @param user ユーザー
     * @return アクセストークン
     */
    public String generateAccessToken(User user) {
        return encode(user, TYPE_ACCESS, properties.accessTokenExpiration(), List.of(user.getRole().name()));
    }

    /**
     * リフレッシュトークンを発行する。ロールは含めない。
     *
     * @param user ユーザー
     * @return リフレッシュトークン
     */
    public String generateRefreshToken(User user) {
        return encode(user, TYPE_REFRESH, properties.refreshTokenExpiration(), null);
    }

    /**
     * リフレッシュトークンの署名、有効期限、種類を検証する。
     *
     * @param token リフレッシュトークン
     * @return トークンの subject（メールアドレス）
     * @throws JwtException トークンが不正、期限切れ、またはリフレッシュトークンではない場合
     */
    public String validateRefreshToken(String token) {
        Jwt jwt = decoder.decode(token);
        if (!TYPE_REFRESH.equals(jwt.getClaimAsString(CLAIM_TOKEN_TYPE))) {
            throw new JwtException(MSG_NOT_REFRESH_TOKEN);
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
