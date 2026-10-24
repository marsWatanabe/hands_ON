package com.aojt.mgmt.repository.common;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.util.Locale;

/**
 * 検索条件（{@link org.springframework.data.jpa.domain.Specification}）の共通部品。
 */
public final class SpecificationSupport {

    /** LIKE のエスケープ文字。 */
    private static final char LIKE_ESCAPE = '\\';

    private SpecificationSupport() {
    }

    /**
     * 部分一致の条件を作る。検索語の {@code %}、{@code _}、{@code \} はワイルドカードにせず、文字として扱う。
     * 大文字小文字は区別しない。
     *
     * @param cb クライテリアビルダー
     * @param field 対象の項目
     * @param value 検索語
     * @return 部分一致の条件
     */
    public static Predicate contains(CriteriaBuilder cb, Expression<String> field, String value) {
        String escaped = value.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return cb.like(cb.lower(field), "%" + escaped + "%", LIKE_ESCAPE);
    }

    /**
     * 検索語が指定されているか（null でも空白だけでもない）を返す。
     *
     * @param value 検索語
     * @return 指定されていれば true
     */
    public static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
