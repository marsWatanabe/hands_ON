package com.aojt.mgmt.config;

import com.aojt.mgmt.model.Role;
import com.aojt.mgmt.service.jwt.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security の設定（JWT による認証・認可）。
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private static final String AUTHORITY_PREFIX = "ROLE_";

    /**
     * セキュリティフィルターチェーン。
     *
     * <p>{@code /api/v1/auth/**} と Swagger は未認証で呼べる。{@code /api/v1/centers/**} は USER 権限以上、
     * それ以外は認証済みであること。H2 コンソールは {@code spring.h2.console.enabled=true} のときだけ開く（開発用）。
     *
     * @param http HTTP セキュリティ
     * @param jwtDecoder JWT デコーダー
     * @param errorHandlers 401・403 を共通形式で返すハンドラ
     * @param h2ConsoleEnabled H2 コンソールが有効か
     * @return フィルターチェーン
     * @throws Exception 設定に失敗した場合
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder,
            SecurityErrorHandlers errorHandlers,
            @Value("${spring.h2.console.enabled:false}") boolean h2ConsoleEnabled) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/api/v1/auth/**").permitAll()
                            .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                    if (h2ConsoleEnabled) {
                        // 開発用。spring.h2.console.settings.web-allow-others=false でローカルからのみに限定される。
                        auth.requestMatchers("/h2-console/**").permitAll();
                    }
                    auth.requestMatchers("/api/v1/centers/**").hasAnyRole(Role.USER.name(), Role.ADMIN.name())
                            .anyRequest().authenticated();
                })
                // H2 コンソールは同一オリジンのフレームで表示される。
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(errorHandlers)
                        .accessDeniedHandler(errorHandlers))
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(errorHandlers)
                        .accessDeniedHandler(errorHandlers));
        return http.build();
    }

    /**
     * {@code roles} クレーム（例: {@code ["USER"]}）を {@code ROLE_*} の権限に変換する。
     *
     * @return JWT 認証コンバーター
     */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(JwtService.CLAIM_ROLES);
        authorities.setAuthorityPrefix(AUTHORITY_PREFIX);
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    /**
     * パスワードのエンコーダー（BCrypt）。
     *
     * @return パスワードエンコーダー
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
