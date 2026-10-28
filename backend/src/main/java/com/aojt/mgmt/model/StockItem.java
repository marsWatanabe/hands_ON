package com.aojt.mgmt.model;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/**
 * 在庫の共通項目。部品在庫（{@code parts_stock_info}）と製品在庫（{@code products_stock_info}）は
 * 同じ列構成のため、共通の項目をここにまとめる。
 *
 * <p>削除は論理削除（{@code delete_flag}）で行う。
 */
@MappedSuperclass
public abstract class StockItem {

    /** 削除フラグ: 未削除。 */
    public static final int NOT_DELETED = 0;

    /** 削除フラグ: 削除済み。 */
    public static final int DELETED = 1;

    /** 在庫ID（自動採番）。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_id")
    private Integer stockId;

    /** カテゴリ。 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    /** 名称（255文字）。 */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * 有効な名称。未削除の間は {@link #name} と同じ値、削除済みは null。
     * 一意制約 {@code (center_id, active_name)} が、同じセンター内の未削除どうしの名称の重複を防ぐ（V5）。
     */
    @Column(name = "active_name", length = 255)
    private String activeName;

    /** 在庫センター。 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id")
    private Center center;

    /** 説明（255文字）。 */
    @Column(name = "description", length = 255)
    private String description;

    /** 数量（個）。 */
    @Column(name = "amount")
    private Integer amount;

    /** 削除フラグ（0: 未削除、1: 削除済み）。 */
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "delete_flag", nullable = false)
    private Integer deleteFlag = NOT_DELETED;

    /** 作成日時。 */
    @CreationTimestamp
    @Column(name = "create_date", updatable = false)
    private LocalDateTime createDate;

    /** 更新日時。 */
    @UpdateTimestamp
    @Column(name = "update_date")
    private LocalDateTime updateDate;

    /**
     * 在庫IDを返す。
     *
     * @return 在庫ID
     */
    public Integer getStockId() {
        return stockId;
    }

    /**
     * カテゴリを返す。
     *
     * @return カテゴリ。未設定は null
     */
    public Category getCategory() {
        return category;
    }

    /**
     * カテゴリを設定する。
     *
     * @param category カテゴリ
     */
    public void setCategory(Category category) {
        this.category = category;
    }

    /**
     * 名称を返す。
     *
     * @return 名称
     */
    public String getName() {
        return name;
    }

    /**
     * 名称を設定する。有効な名称（{@link #getActiveName()}）も合わせて更新する。
     *
     * @param name 名称
     */
    public void setName(String name) {
        this.name = name;
        syncActiveName();
    }

    /**
     * 有効な名称を返す。
     *
     * @return 未削除なら名称、削除済みなら null
     */
    public String getActiveName() {
        return activeName;
    }

    /**
     * 在庫センターを返す。
     *
     * @return 在庫センター。未設定は null
     */
    public Center getCenter() {
        return center;
    }

    /**
     * 在庫センターを設定する。
     *
     * @param center 在庫センター
     */
    public void setCenter(Center center) {
        this.center = center;
    }

    /**
     * 説明を返す。
     *
     * @return 説明。未設定は null
     */
    public String getDescription() {
        return description;
    }

    /**
     * 説明を設定する。
     *
     * @param description 説明
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 数量を返す。
     *
     * @return 数量（個）。未設定は null
     */
    public Integer getAmount() {
        return amount;
    }

    /**
     * 数量を設定する。
     *
     * @param amount 数量（個）
     */
    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    /**
     * 削除フラグを返す。
     *
     * @return 削除フラグ（0: 未削除、1: 削除済み）
     */
    public Integer getDeleteFlag() {
        return deleteFlag;
    }

    /**
     * 作成日時を返す。
     *
     * @return 作成日時
     */
    public LocalDateTime getCreateDate() {
        return createDate;
    }

    /**
     * 更新日時を返す。
     *
     * @return 更新日時
     */
    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    /** 論理削除する（{@code delete_flag = 1}）。有効な名称は null にして、同じ名称を再び登録できるようにする。 */
    public void markDeleted() {
        this.deleteFlag = DELETED;
        syncActiveName();
    }

    /** 有効な名称を、削除フラグと名称から求め直す。 */
    private void syncActiveName() {
        this.activeName = Integer.valueOf(NOT_DELETED).equals(deleteFlag) ? name : null;
    }
}
