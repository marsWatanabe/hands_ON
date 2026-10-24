package com.aojt.mgmt.repository.stock;

import com.aojt.mgmt.model.PartsStock;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 部品在庫のリポジトリ。検索条件は {@link StockSpecifications} で組み立てる。
 */
public interface PartsStockRepository extends JpaRepository<PartsStock, Integer>, JpaSpecificationExecutor<PartsStock> {

    /**
     * 条件に一致する部品在庫を、カテゴリとセンターを結合して 1 回で取得する（1 行ずつの追加取得を避ける）。
     *
     * @param spec 検索条件
     * @param pageable ページング、ソート
     * @return 部品在庫のページ
     */
    @Override
    @EntityGraph(attributePaths = {"category", "center"})
    Page<PartsStock> findAll(Specification<PartsStock> spec, Pageable pageable);

    /**
     * 削除フラグを指定して、部品在庫を検索する。
     *
     * @param stockId 在庫ID
     * @param deleteFlag 削除フラグ
     * @return 該当する部品在庫
     */
    Optional<PartsStock> findByStockIdAndDeleteFlag(Integer stockId, Integer deleteFlag);

    /**
     * 同じセンターに、同じ名称の未削除の部品在庫があるかを確認する。
     *
     * @param centerId 在庫センターID
     * @param name 名称
     * @param deleteFlag 削除フラグ（通常は未削除）
     * @return あれば true
     */
    boolean existsByCenter_CenterIdAndNameAndDeleteFlag(Integer centerId, String name, Integer deleteFlag);

    /**
     * 指定した部品在庫以外で、同じセンターに同じ名称の未削除の部品在庫があるかを確認する。
     *
     * @param centerId 在庫センターID
     * @param name 名称
     * @param deleteFlag 削除フラグ（通常は未削除）
     * @param stockId 除外する在庫ID
     * @return あれば true
     */
    boolean existsByCenter_CenterIdAndNameAndDeleteFlagAndStockIdNot(
            Integer centerId, String name, Integer deleteFlag, Integer stockId);

    /**
     * カテゴリを参照する未削除の部品在庫があるかを確認する。
     *
     * @param categoryId カテゴリID
     * @param deleteFlag 削除フラグ（通常は未削除）
     * @return あれば true
     */
    boolean existsByCategory_CategoryIdAndDeleteFlag(Integer categoryId, Integer deleteFlag);
}
