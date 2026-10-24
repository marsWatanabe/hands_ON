package com.aojt.mgmt.service.common;

import com.aojt.mgmt.dto.common.PagingRequest;
import org.springframework.data.domain.Sort;

/**
 * 一覧取得（ページング、ソート）の共通部品。
 */
public final class PagingSupport {

    /** Hibernate が整数で扱えるページオフセットの上限。 */
    private static final long MAX_JPA_OFFSET = Integer.MAX_VALUE;

    private PagingSupport() {
    }

    /**
     * ページ番号と件数から求めるオフセットが、DB へページ検索を発行できないほど大きいかを返す。
     *
     * @param request ページングの指定
     * @return 大きすぎれば true（空のページを返す）
     */
    public static boolean exceedsJpaOffset(PagingRequest request) {
        long offset = ((long) request.getPage() - 1) * request.getSize();
        return offset > MAX_JPA_OFFSET;
    }

    /**
     * ソート順（{@code asc} または {@code desc}。大文字小文字を区別しない）を {@link Sort.Direction} にする。
     *
     * @param request ページングの指定
     * @return ソートの向き
     */
    public static Sort.Direction direction(PagingRequest request) {
        return Sort.Direction.fromString(request.getOrder());
    }
}
