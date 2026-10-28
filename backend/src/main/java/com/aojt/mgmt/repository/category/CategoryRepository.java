package com.aojt.mgmt.repository.category;

import com.aojt.mgmt.model.Category;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

/**
 * カテゴリのリポジトリ。検索条件は {@link CategorySpecifications} で組み立てる。
 */
public interface CategoryRepository extends JpaRepository<Category, Integer>, JpaSpecificationExecutor<Category> {

    /**
     * 削除フラグを指定して、カテゴリを検索する。
     *
     * @param categoryId カテゴリID
     * @param deleteFlag 削除フラグ（{@link Category#NOT_DELETED} または {@link Category#DELETED}）
     * @return 該当するカテゴリ
     */
    Optional<Category> findByCategoryIdAndDeleteFlag(Integer categoryId, Integer deleteFlag);

    /**
     * 削除フラグを指定して、カテゴリを行ロック（{@code SELECT ... FOR UPDATE}）付きで検索する。
     *
     * <p>カテゴリの削除・種別変更と、そのカテゴリを使う在庫の登録・更新が同時に起きたときに、
     * 「使用中のカテゴリを削除する」「削除済みのカテゴリを参照する在庫ができる」といった矛盾を防ぐ。
     * 論理削除では外部キー制約が働かないため、カテゴリの行を先にロックして直列にする。
     * トランザクション内で呼び出すこと。
     *
     * @param categoryId カテゴリID
     * @param deleteFlag 削除フラグ（{@link Category#NOT_DELETED} または {@link Category#DELETED}）
     * @return 該当するカテゴリ（ロックを取得済み）
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Category> findWithLockByCategoryIdAndDeleteFlag(Integer categoryId, Integer deleteFlag);

    /**
     * 同じ種別に、同じカテゴリ名の未削除のカテゴリがあるかを確認する。
     *
     * @param categoryType カテゴリ種別
     * @param categoryName カテゴリ名
     * @param deleteFlag 削除フラグ（通常は {@link Category#NOT_DELETED}）
     * @return あれば true
     */
    boolean existsByCategoryTypeAndCategoryNameAndDeleteFlag(
            String categoryType, String categoryName, Integer deleteFlag);

    /**
     * 指定したカテゴリ以外で、同じ種別に同じカテゴリ名の未削除のカテゴリがあるかを確認する。
     *
     * @param categoryType カテゴリ種別
     * @param categoryName カテゴリ名
     * @param deleteFlag 削除フラグ（通常は {@link Category#NOT_DELETED}）
     * @param categoryId 除外するカテゴリID
     * @return あれば true
     */
    boolean existsByCategoryTypeAndCategoryNameAndDeleteFlagAndCategoryIdNot(
            String categoryType, String categoryName, Integer deleteFlag, Integer categoryId);
}
