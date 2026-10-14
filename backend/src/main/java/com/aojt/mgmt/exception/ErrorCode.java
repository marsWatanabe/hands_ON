package com.aojt.mgmt.exception;

/**
 * エラーレスポンスのエラーコード。
 *
 * <p>{@code { "error": { "code": ... } }} の {@code code} に、{@link #name()} が入る。
 */
public enum ErrorCode {

    /** 入力値の検証エラー（HTTP 400）。 */
    VALIDATION_ERROR,

    /** 業務ルール違反（HTTP 400）。 */
    BUSINESS_ERROR,

    /** 認証エラー（HTTP 401）。 */
    AUTH_ERROR,

    /** 権限エラー（HTTP 403）。 */
    FORBIDDEN,

    /** リソースが存在しない（HTTP 404）。 */
    NOT_FOUND,

    /** 重複などの競合（HTTP 409）。 */
    CONFLICT,

    /** サーバー内部エラー（HTTP 500）。 */
    INTERNAL_ERROR
}
