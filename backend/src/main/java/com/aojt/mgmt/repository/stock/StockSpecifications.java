package com.aojt.mgmt.repository.stock;

import static com.aojt.mgmt.repository.common.SpecificationSupport.contains;
import static com.aojt.mgmt.repository.common.SpecificationSupport.hasText;

import com.aojt.mgmt.model.ProductsStock;
import com.aojt.mgmt.model.StockItem;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * 在庫（部品在庫、製品在庫）の検索条件（{@link Specification}）。JPA のパラメータ化クエリになり、SQL を連結しない。
 */
public final class StockSpecifications {

    private StockSpecifications() {
    }

    /**
     * 一覧の検索条件を組み立てる。論理削除済みは常に除外し、指定された条件は AND で結合する。
     *
     * @param categoryId カテゴリID（完全一致）。null なら条件にしない
     * @param centerId 在庫センターID（完全一致）。null なら条件にしない
     * @param name 名称（部分一致、大文字小文字を区別しない）。null か空白のみなら条件にしない
     * @param amountMin 数量の下限（以上）。null なら条件にしない
     * @param amountMax 数量の上限（以下）。null なら条件にしない
     * @param <T> 在庫の型
     * @return 検索条件
     */
    public static <T extends StockItem> Specification<T> search(
            Integer categoryId, Integer centerId, String name, Integer amountMin, Integer amountMax) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleteFlag"), StockItem.NOT_DELETED));
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("categoryId"), categoryId));
            }
            if (centerId != null) {
                predicates.add(cb.equal(root.get("center").get("centerId"), centerId));
            }
            if (hasText(name)) {
                predicates.add(contains(cb, root.get("name"), name));
            }
            if (amountMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<Integer>get("amount"), amountMin));
            }
            if (amountMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<Integer>get("amount"), amountMax));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * 製品在庫の検索条件に、製品コードの部分一致を追加する。
     *
     * @param base 在庫の検索条件
     * @param productCode 製品コード（部分一致、大文字小文字を区別しない）。null か空白のみなら追加しない
     * @return 製品コードの条件を追加した検索条件
     */
    public static Specification<ProductsStock> withProductCode(Specification<ProductsStock> base, String productCode) {
        if (!hasText(productCode)) {
            return base;
        }
        return base.and((root, query, cb) -> contains(cb, root.get("productCode"), productCode));
    }
}
