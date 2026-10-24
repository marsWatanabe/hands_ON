package com.aojt.mgmt.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/**
 * カテゴリ（テーブル {@code category_info}）。部品と製品のカテゴリを共通で管理し、種別で区分する。
 *
 * <p>削除は論理削除（{@code delete_flag}）で行う。
 */
@Entity
@Table(name = "category_info")
public class Category {

    /** 削除フラグ: 未削除。 */
    public static final int NOT_DELETED = 0;

    /** 削除フラグ: 削除済み。 */
    public static final int DELETED = 1;

    /** カテゴリ種別: 部品。 */
    public static final String TYPE_PART = "part";

    /** カテゴリ種別: 製品。 */
    public static final String TYPE_PRODUCT = "product";

    /** カテゴリID（自動採番）。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Integer categoryId;

    /** カテゴリ種別（{@code part} または {@code product}）。 */
    @Column(name = "category_type", nullable = false, length = 10)
    private String categoryType;

    /** カテゴリ名（20文字）。 */
    @Column(name = "category_name", nullable = false, length = 20)
    private String categoryName;

    /** 説明（255文字）。 */
    @Column(name = "description", length = 255)
    private String description;

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
     * カテゴリIDを返す。
     *
     * @return カテゴリID
     */
    public Integer getCategoryId() {
        return categoryId;
    }

    /**
     * カテゴリ種別を返す。
     *
     * @return {@code part} または {@code product}
     */
    public String getCategoryType() {
        return categoryType;
    }

    /**
     * カテゴリ種別を設定する。
     *
     * @param categoryType {@code part} または {@code product}
     */
    public void setCategoryType(String categoryType) {
        this.categoryType = categoryType;
    }

    /**
     * カテゴリ名を返す。
     *
     * @return カテゴリ名
     */
    public String getCategoryName() {
        return categoryName;
    }

    /**
     * カテゴリ名を設定する。
     *
     * @param categoryName カテゴリ名
     */
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
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

    /** 論理削除する（{@code delete_flag = 1}）。 */
    public void markDeleted() {
        this.deleteFlag = DELETED;
    }
}
