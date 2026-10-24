package com.aojt.mgmt.dto.common;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 一覧取得 API の共通レスポンス。{@code { "data": [...], "meta": {...} }} の形で返る。
 *
 * @param data 結果の一覧
 * @param meta ページング情報
 * @param <T> 一覧の要素の型
 */
public record PageResponse<T>(List<T> data, Meta meta) {

    /**
     * ページング情報。
     *
     * @param total 総件数
     * @param page ページ番号（1 始まり）
     * @param size 1 ページの件数
     * @param totalPages 総ページ数
     */
    public record Meta(long total, int page, int size, int totalPages) {
    }

    /**
     * Spring の {@link Page}（0 始まり）を、API の形式（1 始まり）に変換する。
     *
     * @param page 変換元のページ
     * @param <T> 要素の型
     * @return レスポンス
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                new Meta(page.getTotalElements(), page.getNumber() + 1, page.getSize(), page.getTotalPages()));
    }

    /**
     * DB へページ検索を発行できないほど大きいページ番号に対する空レスポンスを生成する。
     *
     * @param total 検索条件に一致する総件数
     * @param page リクエストされたページ番号（1 始まり）
     * @param size 1 ページの件数
     * @param <T> 一覧の要素の型
     * @return 空の一覧と、検索条件に基づくページング情報
     */
    public static <T> PageResponse<T> empty(long total, int page, int size) {
        long totalPages = total == 0 ? 0 : 1 + (total - 1) / size;
        return new PageResponse<>(
                List.of(),
                new Meta(total, page, size, (int) Math.min(totalPages, Integer.MAX_VALUE)));
    }
}
