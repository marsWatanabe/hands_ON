package com.aojt.mgmt.exception;

/**
 * 業務ルール違反を表す例外。HTTP 400（{@link ErrorCode#BUSINESS_ERROR}）になる。
 */
public class BusinessException extends RuntimeException {

    /**
     * 例外を生成する。
     *
     * @param message ユーザー向けのメッセージ
     */
    public BusinessException(String message) {
        super(message);
    }
}
