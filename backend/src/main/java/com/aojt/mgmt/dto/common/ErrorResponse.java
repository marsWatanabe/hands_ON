package com.aojt.mgmt.dto.common;

import com.aojt.mgmt.exception.ErrorCode;
import java.time.LocalDateTime;

/**
 * 共通のエラーレスポンス。{@code { "error": { "code", "message", "timestamp" } }} の形で返る。
 *
 * @param error エラーの内容
 */
public record ErrorResponse(Error error) {

    /**
     * エラーレスポンスを生成する。タイムスタンプは現在時刻になる。
     *
     * @param code エラーコード
     * @param message ユーザー向けのメッセージ
     * @return エラーレスポンス
     */
    public static ErrorResponse of(ErrorCode code, String message) {
        return new ErrorResponse(new Error(code.name(), message, LocalDateTime.now()));
    }

    /**
     * エラーの内容。
     *
     * @param code エラーコード（{@link ErrorCode} の名前）
     * @param message ユーザー向けのメッセージ
     * @param timestamp 発生日時（ISO 8601）
     */
    public record Error(String code, String message, LocalDateTime timestamp) {
    }
}
