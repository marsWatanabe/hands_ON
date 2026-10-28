package com.aojt.mgmt.repository.stock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aojt.mgmt.model.Category;
import com.aojt.mgmt.model.Center;
import com.aojt.mgmt.model.PartsStock;
import com.aojt.mgmt.model.ProductsStock;
import com.aojt.mgmt.model.StockItem;
import com.aojt.mgmt.repository.category.CategoryRepository;
import com.aojt.mgmt.repository.center.CenterRepository;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * {@link PartsStockRepository}、{@link ProductsStockRepository}、{@link StockSpecifications} のテスト。
 * Flyway の開発用データ（部品在庫 20 件、製品在庫 12 件。製品コードは V4 が {@code PRD-000001} から採番）を持つ
 * H2 で、検索条件、結合の取得、論理削除の除外、重複確認、制約違反を確認する。
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class StockRepositoryTest {

    @Autowired
    PartsStockRepository parts;

    @Autowired
    ProductsStockRepository products;

    @Autowired
    CategoryRepository categories;

    @Autowired
    CenterRepository centers;

    @Autowired
    JdbcTemplate jdbc;

    private static final Sort BY_ID = Sort.by("stockId");

    private Category category(String type) {
        return categories.findAll().stream().filter(c -> c.getCategoryType().equals(type)).findFirst().orElseThrow();
    }

    private Center center(String name) {
        return centers.findAll().stream().filter(c -> c.getCenterName().equals(name)).findFirst().orElseThrow();
    }

    private PartsStock savePart(String name, Center center, int amount, boolean deleted) {
        PartsStock s = new PartsStock();
        s.setCategory(category(Category.TYPE_PART));
        s.setCenter(center);
        s.setName(name);
        s.setAmount(amount);
        if (deleted) {
            s.markDeleted();
        }
        return parts.saveAndFlush(s);
    }

    private ProductsStock saveProduct(String code, String name, Center center, boolean deleted) {
        ProductsStock s = new ProductsStock();
        s.setProductCode(code);
        s.setCategory(category(Category.TYPE_PRODUCT));
        s.setCenter(center);
        s.setName(name);
        s.setAmount(1);
        if (deleted) {
            s.markDeleted();
        }
        return products.saveAndFlush(s);
    }

    private List<PartsStock> findParts(Integer categoryId, Integer centerId, String name, Integer min, Integer max) {
        return parts.findAll(StockSpecifications.<PartsStock>search(categoryId, centerId, name, min, max), BY_ID);
    }

    // ---- 部品在庫の検索条件 ----

    /** 条件が無ければ、未削除の全件（開発用データの 20 件）を返すこと。 */
    @Test
    void parts_searchWithoutConditionsReturnsAllNotDeleted() {
        assertEquals(20, findParts(null, null, null, null, null).size());
    }

    /** センターで絞り込めること（東京 10 件、大阪 5 件、名古屋 5 件）。 */
    @Test
    void parts_filtersByCenter() {
        assertEquals(10, findParts(null, center("東京センター").getCenterId(), null, null, null).size());
        assertEquals(5, findParts(null, center("大阪センター").getCenterId(), null, null, null).size());
        assertEquals(5, findParts(null, center("名古屋センター").getCenterId(), null, null, null).size());
    }

    /** カテゴリで絞り込めること（フレームは 2 件）。 */
    @Test
    void parts_filtersByCategory() {
        Integer frameId = categories.findAll().stream()
                .filter(c -> c.getCategoryName().equals("フレーム")).findFirst().orElseThrow().getCategoryId();

        assertEquals(2, findParts(frameId, null, null, null, null).size());
    }

    /** 名称は部分一致で、大文字小文字と前後の空白を無視すること。 */
    @Test
    void parts_matchesNamePartiallyIgnoringCase() {
        assertEquals(2, findParts(null, null, " esc ", null, null).size());
        assertEquals(2, findParts(null, null, "プロペラ", null, null).size());
        assertTrue(findParts(null, null, "存在しない名称", null, null).isEmpty());
    }

    /** 個数は、以上・以下・両方の指定ができ、境界値を含むこと。 */
    @Test
    void parts_filtersByAmountRangeInclusive() {
        assertEquals(3, findParts(null, null, null, 200, null).size());
        assertEquals(2, findParts(null, null, null, null, 30).size());
        assertEquals(5, findParts(null, null, null, 100, 150).size());
        assertEquals(1, findParts(null, null, null, 320, 320).size());
        assertTrue(findParts(null, null, null, 321, null).isEmpty());
    }

    /** 検索語の {@code %} {@code _} はワイルドカードにならないこと。 */
    @Test
    void parts_escapesLikeWildcards() {
        assertTrue(findParts(null, null, "%", null, null).isEmpty());
        assertTrue(findParts(null, null, "_", null, null).isEmpty());
    }

    /** 論理削除済みの在庫は、検索にも取得にも出ないこと。 */
    @Test
    void parts_excludesLogicallyDeleted() {
        PartsStock deleted = savePart("削除済み部品", center("東京センター"), 1, true);

        assertEquals(20, findParts(null, null, null, null, null).size());
        assertTrue(findParts(null, null, "削除済み", null, null).isEmpty());
        assertTrue(parts.findByStockIdAndDeleteFlag(deleted.getStockId(), StockItem.NOT_DELETED).isEmpty());
    }

    /** カテゴリ名とセンター名で並べ替えでき、カテゴリ・センターは 1 回の取得で結合されること。 */
    @Test
    void parts_sortsByJoinedNamesAndFetchesAssociationsTogether() {
        Page<PartsStock> byCategory = parts.findAll(StockSpecifications.<PartsStock>search(null, null, null, null, null),
                PageRequest.of(0, 50, Sort.by("category.categoryName").and(Sort.by("name")).and(BY_ID)));
        List<String> categoryNames = new ArrayList<>();
        for (PartsStock s : byCategory.getContent()) {
            assertTrue(Hibernate.isInitialized(s.getCategory()), "カテゴリは結合して取得する");
            assertTrue(Hibernate.isInitialized(s.getCenter()), "センターは結合して取得する");
            categoryNames.add(s.getCategory().getCategoryName());
        }
        assertEquals(20, categoryNames.size());
        assertEquals(categoryNames.stream().sorted().toList(), categoryNames);

        Page<PartsStock> byCenter = parts.findAll(StockSpecifications.<PartsStock>search(null, null, null, null, null),
                PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "center.centerName").and(BY_ID)));
        List<String> centerNames = byCenter.getContent().stream().map(s -> s.getCenter().getCenterName()).toList();
        assertEquals(centerNames.stream().sorted(java.util.Comparator.reverseOrder()).toList(), centerNames);
    }

    /** ページング: 総件数と総ページ数が正しく、最後のページが端数になること。 */
    @Test
    void parts_paginates() {
        Page<PartsStock> page = parts.findAll(StockSpecifications.<PartsStock>search(null, null, null, null, null),
                PageRequest.of(2, 8, BY_ID));

        assertEquals(20, page.getTotalElements());
        assertEquals(3, page.getTotalPages());
        assertEquals(4, page.getContent().size());
    }

    // ---- 部品在庫の重複・参照の確認 ----

    /** 同じセンターの未削除の同名だけを重複と見なし、別のセンターや削除済みは重複にしないこと。 */
    @Test
    void parts_duplicateNameIsPerCenterAndIgnoresDeleted() {
        Center tokyo = center("東京センター");
        Center osaka = center("大阪センター");

        assertTrue(parts.existsByCenter_CenterIdAndNameAndDeleteFlag(
                tokyo.getCenterId(), "カーボンフレーム 450mm", StockItem.NOT_DELETED));
        assertFalse(parts.existsByCenter_CenterIdAndNameAndDeleteFlag(
                osaka.getCenterId(), "カーボンフレーム 450mm", StockItem.NOT_DELETED));

        savePart("カーボンフレーム 450mm", osaka, 5, false);
        assertTrue(parts.existsByCenter_CenterIdAndNameAndDeleteFlag(
                osaka.getCenterId(), "カーボンフレーム 450mm", StockItem.NOT_DELETED));

        savePart("削除済みの名称", osaka, 5, true);
        assertFalse(parts.existsByCenter_CenterIdAndNameAndDeleteFlag(
                osaka.getCenterId(), "削除済みの名称", StockItem.NOT_DELETED));
    }

    /** 自分自身を除いた重複確認が、更新時に自分の名称を重複と見なさないこと。 */
    @Test
    void parts_duplicateCheckExcludesItself() {
        PartsStock own = savePart("自分の名称", center("東京センター"), 1, false);
        Integer centerId = own.getCenter().getCenterId();

        assertFalse(parts.existsByCenter_CenterIdAndNameAndDeleteFlagAndStockIdNot(
                centerId, "自分の名称", StockItem.NOT_DELETED, own.getStockId()));
        assertTrue(parts.existsByCenter_CenterIdAndNameAndDeleteFlagAndStockIdNot(
                centerId, "自分の名称", StockItem.NOT_DELETED, own.getStockId() + 1000));
    }

    /** カテゴリが未削除の在庫から参照されているかを確認できること。 */
    @Test
    void parts_detectsCategoryInUse() {
        Integer usedId = category(Category.TYPE_PART).getCategoryId();
        Category unused = new Category();
        unused.setCategoryType(Category.TYPE_PART);
        unused.setCategoryName("未使用カテゴリ");
        unused = categories.saveAndFlush(unused);

        assertTrue(parts.existsByCategory_CategoryIdAndDeleteFlag(usedId, StockItem.NOT_DELETED));
        assertFalse(parts.existsByCategory_CategoryIdAndDeleteFlag(unused.getCategoryId(), StockItem.NOT_DELETED));
    }

    // ---- 製品在庫 ----

    /** 開発用データの製品在庫 12 件に、製品コードが {@code PRD-000001} から採番されていること。 */
    @Test
    void products_seedRowsHaveGeneratedProductCodes() {
        List<ProductsStock> all = products.findAll(StockSpecifications.<ProductsStock>search(null, null, null, null, null), BY_ID);

        assertEquals(12, all.size());
        for (ProductsStock p : all) {
            assertEquals(String.format("PRD-%06d", p.getStockId()), p.getProductCode());
        }
    }

    /** 製品コードは部分一致で、大文字小文字を無視して検索できること。 */
    @Test
    void products_filtersByProductCodePartially() {
        Specification<ProductsStock> base = StockSpecifications.<ProductsStock>search(null, null, null, null, null);

        assertEquals(1, products.findAll(StockSpecifications.withProductCode(base, "prd-000012"), BY_ID).size());
        assertEquals(12, products.findAll(StockSpecifications.withProductCode(base, " PRD "), BY_ID).size());
        assertEquals(12, products.findAll(StockSpecifications.withProductCode(base, "  "), BY_ID).size());
        assertTrue(products.findAll(StockSpecifications.withProductCode(base, "ZZZ"), BY_ID).isEmpty());
    }

    /** 製品コードの重複確認は、削除済みを含み、自分自身を除けること。 */
    @Test
    void products_productCodeExistenceIncludesDeletedAndExcludesItself() {
        ProductsStock deleted = saveProduct("PRD-DELETED", "削除済み製品", center("東京センター"), true);

        assertTrue(products.existsByProductCode("PRD-000001"));
        assertTrue(products.existsByProductCode("PRD-DELETED"));
        assertFalse(products.existsByProductCode("PRD-NONE"));
        assertFalse(products.existsByProductCodeAndStockIdNot("PRD-DELETED", deleted.getStockId()));
        assertTrue(products.existsByProductCodeAndStockIdNot("PRD-DELETED", deleted.getStockId() + 1000));
    }

    /** 同じ製品コードの保存は、DB の一意制約で拒否されること。 */
    @Test
    void products_rejectsDuplicateProductCodeByConstraint() {
        saveProduct("PRD-UNIQUE", "製品A", center("東京センター"), false);

        assertThrows(DataIntegrityViolationException.class,
                () -> saveProduct("PRD-UNIQUE", "製品B", center("大阪センター"), false));
    }

    /** 製品コードの無い製品在庫の保存は拒否されること（NOT NULL）。 */
    @Test
    void products_rejectsMissingProductCode() {
        assertThrows(DataIntegrityViolationException.class,
                () -> saveProduct(null, "コードなし", center("東京センター"), false));
    }

    /** 製品在庫も、論理削除済みを検索から除外し、カテゴリとセンターを結合して取得すること。 */
    @Test
    void products_excludesDeletedAndFetchesAssociations() {
        saveProduct("PRD-GONE", "削除された製品", center("東京センター"), true);
        Page<ProductsStock> page = products.findAll(
                StockSpecifications.<ProductsStock>search(null, null, null, null, null), PageRequest.of(0, 50, BY_ID));

        assertEquals(12, page.getTotalElements());
        for (ProductsStock p : page.getContent()) {
            assertTrue(Hibernate.isInitialized(p.getCategory()));
            assertTrue(Hibernate.isInitialized(p.getCenter()));
        }
        assertTrue(products.existsByCategory_CategoryIdAndDeleteFlag(
                category(Category.TYPE_PRODUCT).getCategoryId(), StockItem.NOT_DELETED));
    }

    private String activeNameOf(String table, Integer stockId) {
        return jdbc.queryForObject(
                "SELECT active_name FROM " + table + " WHERE stock_id = ?", String.class, stockId);
    }

    /** 同じセンターに未削除どうしで同じ名称の部品在庫を保存すると、DB の一意制約で拒否されること。 */
    @Test
    void parts_rejectsDuplicateActiveNameInTheSameCenterByConstraint() {
        Center tokyo = center("東京センター");
        savePart("重複確認の部品", tokyo, 1, false);

        assertThrows(DataIntegrityViolationException.class, () -> savePart("重複確認の部品", tokyo, 2, false));
    }

    /** 同じセンターに未削除どうしで同じ名称の製品在庫を保存すると、DB の一意制約で拒否されること。 */
    @Test
    void products_rejectsDuplicateActiveNameInTheSameCenterByConstraint() {
        Center tokyo = center("東京センター");
        saveProduct("PRD-DUP-1", "重複確認の製品", tokyo, false);

        assertThrows(DataIntegrityViolationException.class,
                () -> saveProduct("PRD-DUP-2", "重複確認の製品", tokyo, false));
    }

    /** 名称を変更して、同じセンターの未削除の名称と同じになると、DB の一意制約で拒否されること。 */
    @Test
    void parts_rejectsRenameToAnExistingActiveNameByConstraint() {
        Center tokyo = center("東京センター");
        savePart("名称A", tokyo, 1, false);
        PartsStock other = savePart("名称B", tokyo, 1, false);
        other.setName("名称A");

        assertThrows(DataIntegrityViolationException.class, () -> parts.saveAndFlush(other));
    }

    /** 別のセンター、削除済み、削除後の再登録では、同じ名称を保存できること（部品・製品とも）。 */
    @Test
    void stocks_allowSameNameInAnotherCenterAndForDeletedRows() {
        Center tokyo = center("東京センター");
        Center osaka = center("大阪センター");
        PartsStock first = savePart("共通の部品", tokyo, 1, false);
        savePart("共通の部品", osaka, 1, false);
        savePart("削除済みの部品", tokyo, 1, true);
        savePart("削除済みの部品", tokyo, 1, true);

        first.markDeleted();
        parts.saveAndFlush(first);
        PartsStock again = savePart("共通の部品", tokyo, 1, false);

        assertEquals("共通の部品", activeNameOf("parts_stock_info", again.getStockId()));
        assertNull(activeNameOf("parts_stock_info", first.getStockId()));

        saveProduct("PRD-SAME-1", "共通の製品", tokyo, false);
        saveProduct("PRD-SAME-2", "共通の製品", osaka, false);
        saveProduct("PRD-SAME-3", "削除済みの製品", tokyo, true);
        saveProduct("PRD-SAME-4", "削除済みの製品", tokyo, true);
    }

    /** 有効な名称（active_name）が、名称の変更と論理削除に追従して DB に保存されること。 */
    @Test
    void activeName_followsNameAndDeleteFlagInTheDatabase() {
        PartsStock saved = savePart("旧名", center("東京センター"), 1, false);
        assertEquals("旧名", activeNameOf("parts_stock_info", saved.getStockId()));

        saved.setName("新名");
        parts.saveAndFlush(saved);
        assertEquals("新名", activeNameOf("parts_stock_info", saved.getStockId()));

        saved.markDeleted();
        parts.saveAndFlush(saved);
        assertNull(activeNameOf("parts_stock_info", saved.getStockId()));
    }
}
