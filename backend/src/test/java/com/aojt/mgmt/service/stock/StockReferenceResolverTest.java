package com.aojt.mgmt.service.stock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.aojt.mgmt.exception.BusinessException;
import com.aojt.mgmt.model.Category;
import com.aojt.mgmt.model.Center;
import com.aojt.mgmt.repository.category.CategoryRepository;
import com.aojt.mgmt.repository.center.CenterRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link StockReferenceResolver} の単体テスト。在庫が参照するカテゴリ（存在、削除済み、種別）と
 * センター（存在、削除済み）の検証を確認する。
 */
@ExtendWith(MockitoExtension.class)
class StockReferenceResolverTest {

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    CenterRepository centerRepository;

    @InjectMocks
    StockReferenceResolver resolver;

    private static Category category(String type) {
        Category c = new Category();
        c.setCategoryType(type);
        return c;
    }

    /** 未削除で種別が一致するカテゴリを返すこと。 */
    @Test
    void requireCategory_returnsMatchingCategory() {
        Category part = category(Category.TYPE_PART);
        when(categoryRepository.findByCategoryIdAndDeleteFlag(1, Category.NOT_DELETED)).thenReturn(Optional.of(part));

        assertSame(part, resolver.requireCategory(1, Category.TYPE_PART));
    }

    /** 存在しない、または削除済みのカテゴリは BusinessException になること。 */
    @Test
    void requireCategory_rejectsMissingCategory() {
        when(categoryRepository.findByCategoryIdAndDeleteFlag(9, Category.NOT_DELETED)).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class,
                () -> resolver.requireCategory(9, Category.TYPE_PART));
        assertTrue(e.getMessage().contains("見つかりません"));
    }

    /** 部品在庫に製品用のカテゴリを指定すると、部品用を求めるメッセージの BusinessException になること。 */
    @Test
    void requireCategory_rejectsProductCategoryForParts() {
        when(categoryRepository.findByCategoryIdAndDeleteFlag(11, Category.NOT_DELETED))
                .thenReturn(Optional.of(category(Category.TYPE_PRODUCT)));

        BusinessException e = assertThrows(BusinessException.class,
                () -> resolver.requireCategory(11, Category.TYPE_PART));
        assertEquals("カテゴリは部品用のカテゴリを指定してください。", e.getMessage());
    }

    /** 製品在庫に部品用のカテゴリを指定すると、製品用を求めるメッセージの BusinessException になること。 */
    @Test
    void requireCategory_rejectsPartCategoryForProducts() {
        when(categoryRepository.findByCategoryIdAndDeleteFlag(1, Category.NOT_DELETED))
                .thenReturn(Optional.of(category(Category.TYPE_PART)));

        BusinessException e = assertThrows(BusinessException.class,
                () -> resolver.requireCategory(1, Category.TYPE_PRODUCT));
        assertEquals("カテゴリは製品用のカテゴリを指定してください。", e.getMessage());
    }

    /** 未削除のセンターを返すこと。 */
    @Test
    void requireCenter_returnsCenter() {
        Center center = new Center();
        when(centerRepository.findByCenterIdAndDeleteFlag(2, Center.NOT_DELETED)).thenReturn(Optional.of(center));

        assertSame(center, resolver.requireCenter(2));
    }

    /** 存在しない、または削除済みのセンターは BusinessException になること。 */
    @Test
    void requireCenter_rejectsMissingCenter() {
        when(centerRepository.findByCenterIdAndDeleteFlag(9, Center.NOT_DELETED)).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class, () -> resolver.requireCenter(9));
        assertTrue(e.getMessage().startsWith("在庫センター"));
    }
}
