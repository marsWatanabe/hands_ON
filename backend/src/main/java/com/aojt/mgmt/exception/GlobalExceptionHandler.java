package com.aojt.mgmt.exception;

import com.aojt.mgmt.dto.common.ErrorResponse;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * API 全体の例外を、共通のエラーレスポンス形式に変換するハンドラ。
 *
 * <p>形式は {@code { "error": { "code", "message", "timestamp" } }}。
 * 想定外の例外の詳細（スタックトレースなど）はクライアントに返さず、ログにだけ出す。
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String MSG_ACCESS_DENIED = "権限がありません。";
    private static final String MSG_CONFLICT = "データが重複しているか、競合しています。";
    private static final String MSG_INTERNAL = "サーバー内部でエラーが発生しました。";
    private static final String MSG_BAD_REQUEST = "リクエストが不正です。";
    private static final String MSG_UNREADABLE = "リクエストの形式が不正です。";
    private static final String MSG_NOT_FOUND_RESOURCE = "リソースが見つかりません。";
    private static final String MSG_BINDING_FAILURE = "形式が不正です";

    /**
     * リソース未存在を 404 に変換する。
     *
     * @param ex 例外
     * @return エラーレスポンス
     */
    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, ex.getMessage());
    }

    /**
     * 業務ルール違反を 400 に変換する。
     *
     * @param ex 例外
     * @return エラーレスポンス
     */
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        return build(HttpStatus.BAD_REQUEST, ErrorCode.BUSINESS_ERROR, ex.getMessage());
    }

    /**
     * 重複・競合を 409 に変換する。DB の制約違反は詳細を返さない。
     *
     * @param ex 例外（{@link ConflictException} または {@link DataIntegrityViolationException}）
     * @return エラーレスポンス
     */
    @ExceptionHandler({ConflictException.class, DataIntegrityViolationException.class})
    ResponseEntity<ErrorResponse> handleConflict(Exception ex) {
        String message = ex instanceof ConflictException ? ex.getMessage() : MSG_CONFLICT;
        return build(HttpStatus.CONFLICT, ErrorCode.CONFLICT, message);
    }

    /**
     * 認証エラーを 401 に変換する。
     *
     * @param ex 例外
     * @return エラーレスポンス
     */
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, ErrorCode.AUTH_ERROR, ex.getMessage());
    }

    /**
     * 権限エラーを 403 に変換する。
     *
     * @param ex 例外
     * @return エラーレスポンス
     */
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, MSG_ACCESS_DENIED);
    }

    /**
     * クエリパラメータのバインドエラーを 400 に変換する。
     *
     * @param ex 例外
     * @return エラーレスポンス
     */
    @ExceptionHandler(BindException.class)
    ResponseEntity<ErrorResponse> handleBind(BindException ex) {
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, describe(ex));
    }

    /**
     * 想定外の例外を 500 に変換する。詳細はログにだけ出す。
     *
     * @param ex 例外
     * @return エラーレスポンス
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("想定外のエラーが発生しました", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, MSG_INTERNAL);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, describe(ex)));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        // JSON の解析エラーの詳細（内部の型名など）は返さない。
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, MSG_UNREADABLE));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ErrorResponse response;
        if (statusCode.value() == HttpStatus.NOT_FOUND.value()) {
            response = ErrorResponse.of(ErrorCode.NOT_FOUND, MSG_NOT_FOUND_RESOURCE);
        } else if (statusCode.is4xxClientError()) {
            response = ErrorResponse.of(ErrorCode.VALIDATION_ERROR, MSG_BAD_REQUEST);
        } else {
            response = ErrorResponse.of(ErrorCode.INTERNAL_ERROR, MSG_INTERNAL);
        }
        return ResponseEntity.status(statusCode).headers(headers).body(response);
    }

    private static ResponseEntity<ErrorResponse> build(HttpStatus status, ErrorCode code, String message) {
        return ResponseEntity.status(status).body(ErrorResponse.of(code, message));
    }

    /**
     * 検証エラーを「項目: メッセージ」の形で連結する（項目名順）。
     *
     * @param ex バインド例外
     * @return 連結したメッセージ
     */
    private static String describe(BindException ex) {
        return ex.getBindingResult().getAllErrors().stream()
                .map(e -> {
                    String field = e instanceof FieldError f ? f.getField() : e.getObjectName();
                    String message = e instanceof FieldError f && f.isBindingFailure()
                            ? MSG_BINDING_FAILURE
                            : e.getDefaultMessage();
                    return field + ": " + message;
                })
                .sorted()
                .collect(Collectors.joining(", "));
    }
}
