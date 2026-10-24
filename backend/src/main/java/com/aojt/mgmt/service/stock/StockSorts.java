package com.aojt.mgmt.service.stock;

import com.aojt.mgmt.dto.common.PagingRequest;
import com.aojt.mgmt.exception.BusinessException;
import com.aojt.mgmt.service.common.PagingSupport;
import java.util.Set;
import org.springframework.data.domain.Sort;

/**
 * 在庫一覧のソートの組み立て。ソート項目は許可リストで検証し、SQL に直接連結しない。
 */
final class StockSorts {

    private static final String MSG_INVALID_SORT = "並び替え項目が不正です。";

    /** 同じ値が並んでもページ間で順序が変わらないようにする、最後のソートキー。 */
    private static final String TIE_BREAKER = "stockId";

    private StockSorts() {
    }

    /**
     * ソート項目を検証し、{@link Sort} にする。
     *
     * <p>カテゴリ名とセンター名は、結合先の項目（{@code category.categoryName}、{@code center.centerName}）で並べる。
     * カテゴリ名で並べるときは、第 2 キーを名称にする（画面仕様の「カテゴリ/名称」の並び）。
     *
     * @param sortKey ソート項目（許可リストの値）
     * @param request ソート順を含むページングの指定
     * @param allowed この API で使える並び替え項目
     * @return ソート
     * @throws BusinessException ソート項目が許可リストに無い場合
     */
    static Sort resolve(String sortKey, PagingRequest request, Set<String> allowed) {
        if (!allowed.contains(sortKey)) {
            throw new BusinessException(MSG_INVALID_SORT);
        }
        Sort.Direction direction = PagingSupport.direction(request);
        Sort sort = Sort.by(direction, toProperty(sortKey));
        if ("categoryName".equals(sortKey)) {
            sort = sort.and(Sort.by(Sort.Direction.ASC, "name"));
        }
        if (!TIE_BREAKER.equals(sortKey)) {
            sort = sort.and(Sort.by(Sort.Direction.ASC, TIE_BREAKER));
        }
        return sort;
    }

    private static String toProperty(String sortKey) {
        return switch (sortKey) {
            case "categoryName" -> "category.categoryName";
            case "centerName" -> "center.centerName";
            default -> sortKey;
        };
    }
}
