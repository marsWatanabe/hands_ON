package com.aojt.mgmt.dto.center;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 在庫センター一覧取得のクエリパラメータ。{@code @ModelAttribute} でバインドする。
 *
 * <p>未指定の項目は、既定値（{@code page=1}、{@code size=20}、{@code sort=centerId}、{@code order=desc}）になる。
 */
public class CenterSearchRequest {

    /** ソート項目の許可リスト（正規表現）。サービス層の許可リストと一致させる。 */
    private static final String SORT_PATTERN = "centerId|centerName|address|operationalStatus|maxStorageCapacity"
            + "|currentStorageCapacity|createDate|updateDate";

    @Schema(description = "センター名（部分一致）")
    @Size(max = 20, message = "センター名は20文字以内で入力してください")
    private String centerName;

    @Schema(description = "住所（部分一致）")
    @Size(max = 100, message = "住所は100文字以内で入力してください")
    private String address;

    @Schema(description = "稼働状況（0: 稼働中、1: 稼働停止）")
    @Min(value = 0, message = "稼働状況は 0 または 1 を指定してください")
    @Max(value = 1, message = "稼働状況は 0 または 1 を指定してください")
    private Integer operationalStatus;

    @Schema(description = "ページ番号（1 始まり）", defaultValue = "1")
    @Min(value = 1, message = "ページ番号は 1 以上を指定してください")
    private Integer page = 1;

    @Schema(description = "1 ページの件数（1〜100）", defaultValue = "20")
    @Min(value = 1, message = "件数は 1 以上を指定してください")
    @Max(value = 100, message = "件数は 100 以下を指定してください")
    private Integer size = 20;

    @Schema(defaultValue = "centerId", allowableValues = {"centerId", "centerName", "address",
            "operationalStatus", "maxStorageCapacity", "currentStorageCapacity", "createDate", "updateDate"})
    @Pattern(regexp = SORT_PATTERN, message = "並び替え項目が不正です")
    private String sort = "centerId";

    @Schema(defaultValue = "desc", allowableValues = {"asc", "desc"})
    @Pattern(regexp = "(?i)asc|desc", message = "ソート順は asc または desc を指定してください")
    private String order = "desc";

    /**
     * センター名の検索条件を返す。
     *
     * @return センター名（部分一致）
     */
    public String getCenterName() {
        return centerName;
    }

    /**
     * センター名の検索条件を設定する。
     *
     * @param centerName センター名
     */
    public void setCenterName(String centerName) {
        this.centerName = normalizeSearchText(centerName);
    }

    /**
     * 住所の検索条件を返す。
     *
     * @return 住所（部分一致）
     */
    public String getAddress() {
        return address;
    }

    /**
     * 住所の検索条件を設定する。
     *
     * @param address 住所
     */
    public void setAddress(String address) {
        this.address = normalizeSearchText(address);
    }

    /**
     * 稼働状況の検索条件を返す。
     *
     * @return 稼働状況
     */
    public Integer getOperationalStatus() {
        return operationalStatus;
    }

    /**
     * 稼働状況の検索条件を設定する。
     *
     * @param operationalStatus 稼働状況
     */
    public void setOperationalStatus(Integer operationalStatus) {
        this.operationalStatus = operationalStatus;
    }

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
     * ソート項目を返す。
     *
     * @return ソート項目
     */
    public String getSort() {
        return sort;
    }

    /**
     * ソート項目を設定する。
     *
     * @param sort ソート項目
     */
    public void setSort(String sort) {
        this.sort = sort;
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

    private static String normalizeSearchText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
