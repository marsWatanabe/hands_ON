package com.aojt.mgmt.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

/**
 * 一覧取得のクエリパラメータのうち、ページングとソート順の共通部分。{@code @ModelAttribute} でバインドする。
 *
 * <p>ソート項目（{@code sort}）と、既定のソート順は、リソースごとの検索条件クラスが定める。
 */
public abstract class PagingRequest {

    @Schema(description = "ページ番号（1 始まり）", defaultValue = "1")
    @Min(value = 1, message = "ページ番号は 1 以上を指定してください")
    private Integer page = 1;

    @Schema(description = "1 ページの件数（1〜100）", defaultValue = "20")
    @Min(value = 1, message = "件数は 1 以上を指定してください")
    @Max(value = 100, message = "件数は 100 以下を指定してください")
    private Integer size = 20;

    @Schema(allowableValues = {"asc", "desc"})
    @Pattern(regexp = "(?i)asc|desc", message = "ソート順は asc または desc を指定してください")
    private String order = "desc";

    /**
     * ページ番号を返す。
     *
     * @return ページ番号（1 始まり）
     */
    public Integer getPage() {
        return page;
    }

    /**
     * ページ番号を設定する。
     *
     * @param page ページ番号
     */
    public void setPage(Integer page) {
        this.page = page;
    }

    /**
     * 1 ページの件数を返す。
     *
     * @return 件数
     */
    public Integer getSize() {
        return size;
    }

    /**
     * 1 ページの件数を設定する。
     *
     * @param size 件数
     */
    public void setSize(Integer size) {
        this.size = size;
    }

    /**
     * ソート順を返す。
     *
     * @return {@code asc} または {@code desc}
     */
    public String getOrder() {
        return order;
    }

    /**
     * ソート順を設定する。
     *
     * @param order {@code asc} または {@code desc}
     */
    public void setOrder(String order) {
        this.order = order;
    }

    /**
     * 検索語の前後の空白を除く。空白だけの検索語は、条件なし（null）として扱う。
     *
     * @param value 検索語
     * @return 空白を除いた検索語。条件なしは null
     */
    protected static String normalizeSearchText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
