package com.aojt.mgmt.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * 有効な名称（{@code active_name}）の同期ロジックのテスト。DB の一意制約 {@code (center_id, active_name)} と
 * {@code (category_type, active_name)} が「未削除どうしの重複禁止」になるよう、削除済みの行は null になる。
 */
class ActiveNameTest {

    /** 在庫: 名称を設定すると有効な名称も同じ値になり、変更に追従すること。 */
    @Test
    void stock_activeNameFollowsName() {
        PartsStock stock = new PartsStock();
        assertNull(stock.getActiveName());

        stock.setName("プロペラ");
        assertEquals("プロペラ", stock.getActiveName());

        stock.setName("モーター");
        assertEquals("モーター", stock.getActiveName());
    }

    /** 在庫: 論理削除すると有効な名称が null になり、削除後に名称を変えても null のままであること。 */
    @Test
    void stock_activeNameIsNullAfterLogicalDelete() {
        ProductsStock stock = new ProductsStock();
        stock.setName("災害用ドローン");

        stock.markDeleted();
        assertEquals(StockItem.DELETED, stock.getDeleteFlag());
        assertNull(stock.getActiveName());

        stock.setName("名称変更");
        assertNull(stock.getActiveName());
    }

    /** カテゴリ: 名称の設定と変更に追従すること。 */
    @Test
    void category_activeNameFollowsName() {
        Category category = new Category();
        assertNull(category.getActiveName());

        category.setCategoryName("フレーム");
        assertEquals("フレーム", category.getActiveName());

        category.setCategoryName("プロペラ");
        assertEquals("プロペラ", category.getActiveName());
    }

    /** カテゴリ: 論理削除すると有効な名称が null になり、削除後に名称を変えても null のままであること。 */
    @Test
    void category_activeNameIsNullAfterLogicalDelete() {
        Category category = new Category();
        category.setCategoryName("フレーム");

        category.markDeleted();
        assertEquals(Category.DELETED, category.getDeleteFlag());
        assertNull(category.getActiveName());

        category.setCategoryName("名称変更");
        assertNull(category.getActiveName());
    }
}
