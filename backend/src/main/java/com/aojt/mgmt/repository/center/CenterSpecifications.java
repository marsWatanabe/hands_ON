package com.aojt.mgmt.repository.center;

import com.aojt.mgmt.model.Center;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/**
 * 在庫センターの検索条件（{@link Specification}）。JPA のパラメータ化クエリになり、SQL を連結しない。
 */
public final class CenterSpecifications {

    /** LIKE のエスケープ文字。 */
    private static final char LIKE_ESCAPE = '\\';

    private CenterSpecifications() {
    }

    /**
     * 一覧の検索条件を組み立てる。論理削除済みは常に除外し、指定された条件は AND で結合する。
     *
     * @param centerName センター名（部分一致、大文字小文字を区別しない）。null か空白のみなら条件にしない
     * @param address 住所（部分一致、大文字小文字を区別しない）。null か空白のみなら条件にしない
     * @param operationalStatus 稼働状況（完全一致）。null なら条件にしない
     * @return 検索条件
     */
    public static Specification<Center> search(String centerName, String address, Integer operationalStatus) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleteFlag"), Center.NOT_DELETED));
            if (hasText(centerName)) {
                predicates.add(contains(cb, root.get("centerName"), centerName));
            }
            if (hasText(address)) {
                predicates.add(contains(cb, root.get("address"), address));
            }
            if (operationalStatus != null) {
                predicates.add(cb.equal(root.get("operationalStatus"), operationalStatus));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * 部分一致の条件を作る。検索語の {@code %}、{@code _}、{@code \} はワイルドカードにせず、文字として扱う。
     *
     * @param cb クライテリアビルダー
     * @param field 対象の項目
     * @param value 検索語
     * @return 部分一致の条件
     */
    private static Predicate contains(CriteriaBuilder cb, Expression<String> field, String value) {
        String escaped = value.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return cb.like(cb.lower(field), "%" + escaped + "%", LIKE_ESCAPE);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
