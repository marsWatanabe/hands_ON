package com.aojt.mgmt.repository.category;

import static com.aojt.mgmt.repository.common.SpecificationSupport.contains;
import static com.aojt.mgmt.repository.common.SpecificationSupport.hasText;

import com.aojt.mgmt.model.Category;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * カテゴリの検索条件（{@link Specification}）。JPA のパラメータ化クエリになり、SQL を連結しない。
 */
public final class CategorySpecifications {

    private CategorySpecifications() {
    }

    /**
     * 一覧の検索条件を組み立てる。論理削除済みは常に除外し、指定された条件は AND で結合する。
     *
     * @param categoryType カテゴリ種別（完全一致）。null か空白のみなら条件にしない
     * @param categoryName カテゴリ名（部分一致、大文字小文字を区別しない）。null か空白のみなら条件にしない
     * @return 検索条件
     */
    public static Specification<Category> search(String categoryType, String categoryName) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleteFlag"), Category.NOT_DELETED));
            if (hasText(categoryType)) {
                predicates.add(cb.equal(root.get("categoryType"), categoryType.trim()));
            }
            if (hasText(categoryName)) {
                predicates.add(contains(cb, root.get("categoryName"), categoryName));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
