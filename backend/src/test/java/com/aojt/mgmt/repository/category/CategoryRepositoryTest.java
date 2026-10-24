package com.aojt.mgmt.repository.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aojt.mgmt.model.Category;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

/**
 * {@link CategoryRepository} と {@link CategorySpecifications} のテスト。Flyway の開発用データ（部品 10 件、
 * 製品 6 件）を持つ H2 で、検索条件、論理削除の除外、名称の重複確認を確認する。
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class CategoryRepositoryTest {

    @Autowired
    CategoryRepository repository;

    private Category save(String type, String name, boolean deleted) {
        Category c = new Category();
        c.setCategoryType(type);
        c.setCategoryName(name);
        if (deleted) {
            c.markDeleted();
        }
        return repository.saveAndFlush(c);
    }

    private List<String> names(String type, String name) {
        return repository.findAll(CategorySpecifications.search(type, name), Sort.by("categoryId")).stream()
                .map(Category::getCategoryName).toList();
    }

    /** 条件が無ければ、未削除の全件（開発用データの 16 件）を返すこと。 */
    @Test
    void search_withoutConditionsReturnsAllNotDeleted() {
        assertEquals(16, names(null, null).size());
        assertEquals(16, names("  ", " ").size());
    }

    /** 種別で完全一致の絞り込みができること（部品 10 件、製品 6 件）。 */
    @Test
    void search_filtersByType() {
        assertEquals(10, names("part", null).size());
        assertEquals(6, names("product", null).size());
        assertTrue(names("other", null).isEmpty());
    }

    /** カテゴリ名は部分一致で、前後の空白と大文字小文字を無視すること。 */
    @Test
    void search_matchesNamePartiallyIgnoringCaseAndSpaces() {
        assertEquals(6, names(null, "ドローン").size());
        assertEquals(List.of("GPSモジュール"), names(null, "  gps "));
    }

    /** 種別と名称の条件は AND で結合されること。 */
    @Test
    void search_combinesTypeAndName() {
        assertEquals(List.of(), names("part", "ドローン"));
        assertEquals(6, names("product", "ドローン").size());
    }

    /** 検索語の {@code %} {@code _} はワイルドカードにならず、文字として扱うこと。 */
    @Test
    void search_escapesLikeWildcards() {
        assertTrue(names(null, "%").isEmpty());
        assertTrue(names(null, "_").isEmpty());
        save("part", "100%部品", false);
        assertEquals(List.of("100%部品"), names(null, "%"));
    }

    /** 論理削除済みのカテゴリは、検索にも取得にも出ないこと。 */
    @Test
    void search_excludesLogicallyDeletedCategories() {
        Category deleted = save("part", "削除済みカテゴリ", true);

        assertEquals(16, names(null, null).size());
        assertTrue(names(null, "削除済み").isEmpty());
        assertTrue(repository.findByCategoryIdAndDeleteFlag(deleted.getCategoryId(), Category.NOT_DELETED).isEmpty());
        assertTrue(repository.findByCategoryIdAndDeleteFlag(deleted.getCategoryId(), Category.DELETED).isPresent());
    }

    /** 同じ種別の未削除の同名だけを重複として扱い、別の種別と削除済みは重複にしないこと。 */
    @Test
    void existsByTypeAndName_countsOnlyNotDeletedInTheSameType() {
        assertTrue(repository.existsByCategoryTypeAndCategoryNameAndDeleteFlag(
                "part", "フレーム", Category.NOT_DELETED));
        assertFalse(repository.existsByCategoryTypeAndCategoryNameAndDeleteFlag(
                "product", "フレーム", Category.NOT_DELETED));

        save("product", "フレーム", false);
        assertTrue(repository.existsByCategoryTypeAndCategoryNameAndDeleteFlag(
                "product", "フレーム", Category.NOT_DELETED));

        save("part", "削除予定", true);
        assertFalse(repository.existsByCategoryTypeAndCategoryNameAndDeleteFlag(
                "part", "削除予定", Category.NOT_DELETED));
    }

    /** 自分自身を除いた重複確認が、更新時に自分の名前を重複と見なさないこと。 */
    @Test
    void existsByTypeAndNameAndIdNot_excludesItself() {
        Category frame = repository.findAll(CategorySpecifications.search("part", "フレーム"), Sort.by("categoryId"))
                .get(0);

        assertFalse(repository.existsByCategoryTypeAndCategoryNameAndDeleteFlagAndCategoryIdNot(
                "part", "フレーム", Category.NOT_DELETED, frame.getCategoryId()));
        assertTrue(repository.existsByCategoryTypeAndCategoryNameAndDeleteFlagAndCategoryIdNot(
                "part", "フレーム", Category.NOT_DELETED, frame.getCategoryId() + 1000));
    }

    /** 作成日時と更新日時が自動で設定され、削除フラグの既定が 0 であること。 */
    @Test
    void save_setsTimestampsAndDefaultDeleteFlag() {
        Category saved = save("product", "新カテゴリ", false);

        assertEquals(Category.NOT_DELETED, saved.getDeleteFlag());
        assertTrue(saved.getCreateDate() != null && saved.getUpdateDate() != null);
    }
}
