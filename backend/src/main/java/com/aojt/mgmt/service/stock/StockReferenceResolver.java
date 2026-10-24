package com.aojt.mgmt.service.stock;

import com.aojt.mgmt.exception.BusinessException;
import com.aojt.mgmt.model.Category;
import com.aojt.mgmt.model.Center;
import com.aojt.mgmt.repository.category.CategoryRepository;
import com.aojt.mgmt.repository.center.CenterRepository;
import org.springframework.stereotype.Component;

/**
 * 在庫の登録・更新で参照する、カテゴリとセンターを検証して取得する。
 */
@Component
public class StockReferenceResolver {

    private static final String MSG_CATEGORY_NOT_FOUND = "カテゴリが見つかりません。ID: ";
    private static final String MSG_CATEGORY_NOT_PART = "カテゴリは部品用のカテゴリを指定してください。";
    private static final String MSG_CATEGORY_NOT_PRODUCT = "カテゴリは製品用のカテゴリを指定してください。";
    private static final String MSG_CENTER_NOT_FOUND = "在庫センターが見つかりません。ID: ";

    private final CategoryRepository categoryRepository;
    private final CenterRepository centerRepository;

    /**
     * 生成する。
     *
     * @param categoryRepository カテゴリのリポジトリ
     * @param centerRepository 在庫センターのリポジトリ
     */
    public StockReferenceResolver(CategoryRepository categoryRepository, CenterRepository centerRepository) {
        this.categoryRepository = categoryRepository;
        this.centerRepository = centerRepository;
    }

    /**
     * 在庫のカテゴリを取得する。未削除で、期待する種別のカテゴリでなければならない。
     *
     * @param categoryId カテゴリID
     * @param expectedType 期待する種別（{@link Category#TYPE_PART} または {@link Category#TYPE_PRODUCT}）
     * @return カテゴリ
     * @throws BusinessException 存在しない、削除済み、または種別が違う場合
     */
    public Category requireCategory(Integer categoryId, String expectedType) {
        Category category = categoryRepository.findByCategoryIdAndDeleteFlag(categoryId, Category.NOT_DELETED)
                .orElseThrow(() -> new BusinessException(MSG_CATEGORY_NOT_FOUND + categoryId));
        if (!expectedType.equals(category.getCategoryType())) {
            throw new BusinessException(
                    Category.TYPE_PART.equals(expectedType) ? MSG_CATEGORY_NOT_PART : MSG_CATEGORY_NOT_PRODUCT);
        }
        return category;
    }

    /**
     * 在庫のセンターを取得する。未削除でなければならない。
     *
     * @param centerId 在庫センターID
     * @return 在庫センター
     * @throws BusinessException 存在しない、または削除済みの場合
     */
    public Center requireCenter(Integer centerId) {
        return centerRepository.findByCenterIdAndDeleteFlag(centerId, Center.NOT_DELETED)
                .orElseThrow(() -> new BusinessException(MSG_CENTER_NOT_FOUND + centerId));
    }
}
