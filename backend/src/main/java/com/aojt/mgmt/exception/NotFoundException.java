package com.aojt.mgmt.exception;

/**
 * リソースが存在しない（または削除済み）ことを表す例外。HTTP 404 になる。
 */
public class NotFoundException extends RuntimeException {

    /**
     * 例外を生成する。
     *
     * @param message ユーザー向けのメッセージ
     */
    public NotFoundException(String message) {
        super(message);
    }
}
