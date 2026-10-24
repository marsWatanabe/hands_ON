package com.aojt.mgmt.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * 動作確認用のデモデータ（db/seed-demo/V3）の検証。
 *
 * <p>通常のテストはデモデータを読み込まない（application-test.yml）。ここでだけ読み込み、
 * 件数、紐づき、API の入力規則との整合を確認する。
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.flyway.locations="
        + "classpath:db/migration,classpath:db/seed-dev,classpath:db/seed-demo")
class DemoSeedDataTest {

    private static final Pattern POST_CODE = Pattern.compile("^\\d{3}-\\d{4}$");
    private static final Pattern PHONE = Pattern.compile("^\\d{2,5}-\\d{1,4}-\\d{4}$");

    @Autowired
    JdbcTemplate jdbc;

    private int count(String sql) {
        Integer n = jdbc.queryForObject(sql, Integer.class);
        return n == null ? 0 : n;
    }

    /** 在庫センターが 22 件（V2 の 3 件 + デモの 19 件）になり、在庫の件数も増えること。 */
    @Test
    void centersAndStocksAreIncreased() {
        assertEquals(22, count("SELECT COUNT(*) FROM center_info"));
        assertEquals(20 + 38, count("SELECT COUNT(*) FROM parts_stock_info"));
        assertEquals(12 + 19, count("SELECT COUNT(*) FROM products_stock_info"));
        assertEquals(16, count("SELECT COUNT(*) FROM category_info"));
    }

    /** すべてのセンターに、部品在庫と製品在庫が 1 件以上紐づくこと。 */
    @Test
    void everyCenterHasPartsAndProducts() {
        assertEquals(0, count("SELECT COUNT(*) FROM center_info c WHERE NOT EXISTS"
                + " (SELECT 1 FROM parts_stock_info p WHERE p.center_id = c.center_id)"));
        assertEquals(0, count("SELECT COUNT(*) FROM center_info c WHERE NOT EXISTS"
                + " (SELECT 1 FROM products_stock_info p WHERE p.center_id = c.center_id)"));
    }

    /** 在庫のカテゴリ種別が正しく（部品在庫は part、製品在庫は product）、センターの参照が欠けていないこと。 */
    @Test
    void stocksReferenceCorrectCategoryTypesAndCenters() {
        assertEquals(0, count("SELECT COUNT(*) FROM parts_stock_info p JOIN category_info c"
                + " ON p.category_id = c.category_id WHERE c.category_type <> 'part'"));
        assertEquals(0, count("SELECT COUNT(*) FROM products_stock_info p JOIN category_info c"
                + " ON p.category_id = c.category_id WHERE c.category_type <> 'product'"));
        assertEquals(0, count("SELECT COUNT(*) FROM parts_stock_info WHERE center_id IS NULL"));
        assertEquals(0, count("SELECT COUNT(*) FROM products_stock_info WHERE center_id IS NULL"));
    }

    /** 稼働停止が 3 件、容量が未設定のセンターが 1 件、現在容量が最大容量を超えないこと。 */
    @Test
    void statusAndCapacityVariationsAreIncluded() {
        assertEquals(3, count("SELECT COUNT(*) FROM center_info WHERE operational_status = 1"));
        assertEquals(19, count("SELECT COUNT(*) FROM center_info WHERE operational_status = 0"));
        assertEquals(1, count("SELECT COUNT(*) FROM center_info WHERE max_storage_capacity IS NULL"));
        assertEquals(0, count("SELECT COUNT(*) FROM center_info"
                + " WHERE current_storage_capacity > max_storage_capacity"));
    }

    /** 郵便番号と電話番号が API の入力規則（CenterRequest）の形式に合っていること。 */
    @Test
    void postCodesAndPhoneNumbersMatchApiRules() {
        List<String> postCodes = jdbc.queryForList("SELECT post_code FROM center_info", String.class);
        List<String> phones = jdbc.queryForList("SELECT phone_number FROM center_info", String.class);
        assertEquals(22, postCodes.size());
        postCodes.forEach(p -> assertTrue(POST_CODE.matcher(p).matches(), "郵便番号: " + p));
        phones.forEach(p -> assertTrue(PHONE.matcher(p).matches(), "電話番号: " + p));
    }

    /** センター名が 20 文字以内で、住所が都道府県で始まること（画面の住所分割の対象になる形）。 */
    @Test
    void namesAndAddressesFitTheScreenRules() {
        List<String> names = jdbc.queryForList("SELECT center_name FROM center_info", String.class);
        names.forEach(n -> assertTrue(n.length() <= 20, "センター名: " + n));
        List<String> addresses = jdbc.queryForList("SELECT address FROM center_info", String.class);
        addresses.forEach(a -> assertTrue(a.matches("^(北海道|東京都|大阪府|京都府|.{2,3}県).+"), "住所: " + a));
    }
}
