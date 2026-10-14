package com.aojt.mgmt.config;

import com.aojt.mgmt.dto.common.ErrorResponse;
import com.aojt.mgmt.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * セキュリティフィルターで発生した 401・403 を、共通のエラー形式で返すハンドラ。
 */
@Component
public class SecurityErrorHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final String MSG_UNAUTHORIZED = "認証が必要です。トークンが無い、または無効です。";
    private static final String MSG_FORBIDDEN = "権限がありません。";

    private final ObjectMapper objectMapper;
    private final BearerTokenAuthenticationEntryPoint delegate = new BearerTokenAuthenticationEntryPoint();

    /**
     * ハンドラを生成する。
     *
     * @param objectMapper JSON の変換に使う ObjectMapper
     */
    public SecurityErrorHandlers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 認証されていないリクエストに 401 を返す。
     *
     * @param request リクエスト
     * @param response レスポンス
     * @param authException 認証例外
     * @throws IOException 書き込みに失敗した場合
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        // WWW-Authenticate ヘッダを設定する。本文は下で共通形式に置き換える。
        delegate.commence(request, response, authException);
        write(response, HttpStatus.UNAUTHORIZED, ErrorCode.AUTH_ERROR, MSG_UNAUTHORIZED);
    }

    /**
     * 権限の無いリクエストに 403 を返す。
     *
     * @param request リクエスト
     * @param response レスポンス
     * @param accessDeniedException 権限例外
     * @throws IOException 書き込みに失敗した場合
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        write(response, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, MSG_FORBIDDEN);
    }

    private void write(HttpServletResponse response, HttpStatus status, ErrorCode code, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(code, message));
    }
}
