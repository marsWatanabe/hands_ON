package com.aojt.mgmt.repository.center;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aojt.mgmt.model.Center;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

/**
 * {@link CenterRepository} と {@link CenterSpecifications} のテスト。Flyway の開発用データ（東京、大阪、名古屋の
 * 3 センター、すべて稼働中）を持つ H2 で、検索条件、論理削除の除外、制約違反を確認する。
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class CenterRepositoryTest {

    @Autowired
    CenterRepository repository;

    private Center save(String name, String address, int status, boolean deleted) {
        Center c = new Center();
        c.setCenterName(name);
        c.setPostCode("100-0001");
        c.setAddress(address);
        c.setPhoneNumber("03-1234-5678");
        c.setManagerName("管理者");
        c.setOperationalStatus(status);
        if (deleted) {
            c.markDeleted();
        }
        return repository.saveAndFlush(c);
    }

    private List<String> names(String centerName, String address, Integer status) {
        return repository.findAll(CenterSpecifications.search(centerName, address, status)).stream()
                .map(Center::getCenterName).toList();
    }

    // ---- 検索条件 ----

    /** 条件が無ければ、未削除の全件（開発用データの 3 件）を返すこと。 */
    @Test
    void search_withoutConditionsReturnsAllNotDeleted() {
        assertEquals(3, names(null, null, null).size());
    }

    /** 論理削除済みのセンターは、どの条件でも返さないこと。 */
    @Test
    void search_excludesLogicallyDeletedCenters() {
        save("削除済みセンター", "東京都", Center.STATUS_OPERATING, true);

        assertEquals(3, names(null, null, null).size());
        assertTrue(names("削除済み", null, null).isEmpty());
    }

    /** センター名は部分一致で検索できること。 */
    @Test
    void search_matchesCenterNamePartially() {
        assertEquals(List.of("東京センター"), names("東京", null, null));
        assertEquals(3, names("センター", null, null).size());
    }

    /** 住所は部分一致で検索できること。 */
    @Test
    void search_matchesAddressPartially() {
        assertEquals(List.of("大阪センター"), names(null, "大阪府大阪市", null));
    }

    /** 英字は大文字小文字を区別せず、前後の空白は無視すること。 */
    @Test
    void search_isCaseInsensitiveAndTrimsTheKeyword() {
        save("Alpha Hub", "Tokyo", Center.STATUS_OPERATING, false);

        assertEquals(List.of("Alpha Hub"), names("  alpha ", null, null));
        assertEquals(List.of("Alpha Hub"), names("ALPHA HUB", null, null));
    }

    /** 空文字・空白のみの条件は、条件として扱わないこと。 */
    @Test
    void search_ignoresBlankConditions() {
        assertEquals(3, names("", "   ", null).size());
    }

    /** 稼働状況は完全一致で絞り込めること。 */
    @Test
    void search_filtersByOperationalStatus() {
        save("停止センター", "福岡県", Center.STATUS_STOPPED, false);

        assertEquals(List.of("停止センター"), names(null, null, Center.STATUS_STOPPED));
        assertEquals(3, names(null, null, Center.STATUS_OPERATING).size());
    }

    /** 複数の条件は AND で結合されること。 */
    @Test
    void search_combinesConditionsWithAnd() {
        assertEquals(List.of("東京センター"), names("センター", "東京都", Center.STATUS_OPERATING));
        assertTrue(names("東京", "大阪府", null).isEmpty());
    }

    /** 検索語の % と _ はワイルドカードではなく、文字として扱うこと。 */
    @Test
    void search_treatsLikeWildcardsLiterally() {
        save("50%センター", "東京都", Center.STATUS_OPERATING, false);
        save("A_B", "東京都", Center.STATUS_OPERATING, false);
        save("AXB", "東京都", Center.STATUS_OPERATING, false);

        // % が「任意の文字列」なら全件に一致してしまう。文字としての % を含む 1 件だけに一致する。
        assertEquals(List.of("50%センター"), names("%", null, null));
        // _ が「任意の 1 文字」なら AXB にも一致してしまう。
        assertEquals(List.of("A_B"), names("A_B", null, null));
        assertTrue(names("\\", null, null).isEmpty());
    }

    // ---- リポジトリのメソッド ----

    /** 削除フラグを指定して、未削除と削除済みを区別して取得できること。 */
    @Test
    void findByCenterIdAndDeleteFlag_distinguishesDeletedCenters() {
        Center deleted = save("削除済み", "東京都", Center.STATUS_OPERATING, true);
        Center active = save("有効", "東京都", Center.STATUS_OPERATING, false);

        assertFalse(repository.findByCenterIdAndDeleteFlag(deleted.getCenterId(), Center.NOT_DELETED).isPresent());
        assertTrue(repository.findByCenterIdAndDeleteFlag(deleted.getCenterId(), Center.DELETED).isPresent());
        assertTrue(repository.findByCenterIdAndDeleteFlag(active.getCenterId(), Center.NOT_DELETED).isPresent());
    }

    /** センター名の存在確認は、論理削除済みも対象にすること（DB のユニーク制約と同じ）。 */
    @Test
    void existsByCenterName_includesDeletedCenters() {
        save("削除済み", "東京都", Center.STATUS_OPERATING, true);

        assertTrue(repository.existsByCenterName("削除済み"));
        assertTrue(repository.existsByCenterName("東京センター"));
        assertFalse(repository.existsByCenterName("存在しない"));
    }

    /** 自分自身を除いた重複確認ができること。 */
    @Test
    void existsByCenterNameAndCenterIdNot_excludesTheCenterItself() {
        Center tokyo = repository.findAll(CenterSpecifications.search("東京", null, null)).get(0);

        assertFalse(repository.existsByCenterNameAndCenterIdNot("東京センター", tokyo.getCenterId()));
        assertTrue(repository.existsByCenterNameAndCenterIdNot("東京センター", tokyo.getCenterId() + 1000));
    }

    // ---- 制約違反 ----

    /** 同名のセンターは保存できず、制約違反になること。 */
    @Test
    void save_rejectsDuplicateCenterName() {
        assertThrows(DataIntegrityViolationException.class,
                () -> save("東京センター", "東京都", Center.STATUS_OPERATING, false));
    }

    /** 20 文字を超えるセンター名は保存できないこと。 */
    @Test
    void save_rejectsTooLongCenterName() {
        assertThrows(DataIntegrityViolationException.class,
                () -> save("あ".repeat(21), "東京都", Center.STATUS_OPERATING, false));
    }

    /** 20 文字ちょうど（境界値）のセンター名は保存できること。 */
    @Test
    void save_acceptsCenterNameOfExactlyMaxLength() {
        Center saved = save("あ".repeat(20), "東京都", Center.STATUS_OPERATING, false);

        assertEquals(20, saved.getCenterName().length());
    }

    /** 稼働状況が 0・1 以外の値は保存できないこと。 */
    @Test
    void save_rejectsInvalidOperationalStatus() {
        assertThrows(DataIntegrityViolationException.class, () -> save("不正", "東京都", 2, false));
    }
}
