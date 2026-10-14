package com.aojt.mgmt.exception;

/**
 * 重複などの競合を表す例外。HTTP 409 になる。
 */
public class ConflictException extends RuntimeException {

    /**
     * 例外を生成する。
     *
     * @param message ユーザー向けのメッセージ
     */
    public ConflictException(String message) {
        super(message);
    }
}
