package com.aojt.mgmt;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * カテゴリ、部品在庫、製品在庫 API の結合テスト。実際のセキュリティ設定と DB（H2、Flyway の開発用データ）で、
 * 登録から検索、更新、削除までの一連の流れと、カテゴリ・センターとの紐づきの検証を確認する。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StockApiIntegrationTest {

    /** 開発用データの製品カテゴリ（災害用ドローン）の ID。 */
    private static final int SEED_PRODUCT_CATEGORY_ID = 11;

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    String accessToken;
    String refreshToken;
    int tokyoCenterId;

    @BeforeEach
    void registerUserAndFindCenter() throws Exception {
        String body = """
                {"firstname":"Taro","lastname":"Test","email":"%s@example.com","password":"password123"}
                """.formatted(UUID.randomUUID());
        JsonNode node = json(mvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()));
        accessToken = node.get("accessToken").asText();
        refreshToken = node.get("refreshToken").asText();
        tokyoCenterId = json(mvc.perform(auth(get("/api/v1/centers").param("centerName", "東京"))))
                .get("data").get(0).get("centerId").asInt();
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder) {
        return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
    }

    private JsonNode json(ResultActions actions) throws Exception {
        return objectMapper.readTree(actions.andReturn().getResponse().getContentAsString());
    }

    private ResultActions send(MockHttpServletRequestBuilder builder, String body) throws Exception {
        return mvc.perform(auth(builder).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static String categoryJson(String type, String name) {
        return """
                {"categoryType":"%s","categoryName":"%s","description":"結合テスト"}""".formatted(type, name);
    }

    private static String partJson(int categoryId, String name, int centerId, int amount) {
        return """
                {"categoryId":%d,"name":"%s","centerId":%d,"description":"結合テスト","amount":%d}"""
                .formatted(categoryId, name, centerId, amount);
    }

    private static String productJson(String code, int categoryId, String name, int centerId, int amount) {
        return """
                {"productCode":"%s","categoryId":%d,"name":"%s","centerId":%d,"description":"結合テスト","amount":%d}"""
                .formatted(code, categoryId, name, centerId, amount);
    }

    private int createCategory(String type, String name) throws Exception {
        return json(send(post("/api/v1/categories"), categoryJson(type, name)).andExpect(status().isOk()))
                .get("categoryId").asInt();
    }

    // ---- 認証 ----

    /** 3 つの API は、トークンが無ければ 401、リフレッシュトークンでは 403 になること。 */
    @Test
    void endpointsRequireAnAccessToken() throws Exception {
        for (String path : new String[] {"/api/v1/categories", "/api/v1/parts-stocks", "/api/v1/products-stocks"}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("AUTH_ERROR"));
            mvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        }
    }

    // ---- 一覧（開発用データ） ----

    /** カテゴリの一覧は、既定で「種別の降順、カテゴリ名の降順」の順に 16 件あり、種別で絞り込めること。 */
    @Test
    void categoryListUsesSeedDataAndDefaultOrder() throws Exception {
        JsonNode all = json(mvc.perform(auth(get("/api/v1/categories").param("size", "100")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.meta.total", greaterThanOrEqualTo(16))));
        // 他のテストが追加したカテゴリがあっても、種別の降順（製品が先、部品が後）であること
        int last = all.get("data").size() - 1;
        assertEquals("product", all.get("data").get(0).get("categoryType").asText());
        assertEquals("part", all.get("data").get(last).get("categoryType").asText());

        mvc.perform(auth(get("/api/v1/categories").param("categoryType", "part")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.meta.total", greaterThanOrEqualTo(10)));
        mvc.perform(auth(get("/api/v1/categories").param("categoryName", "ドローン")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.meta.total").value(6));
    }

    /** 部品在庫の一覧は、カテゴリ名とセンター名を結合して返し、すべての並び替え項目が動くこと。 */
    @Test
    void partsListReturnsJoinedNamesAndSupportsEverySortKey() throws Exception {
        mvc.perform(auth(get("/api/v1/parts-stocks")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total", greaterThanOrEqualTo(20)))
                .andExpect(jsonPath("$.data[0].categoryName").isString())
                .andExpect(jsonPath("$.data[0].centerName").isString())
                .andExpect(jsonPath("$.data[0].deleteFlag").doesNotExist());

        for (String sort : new String[] {"stockId", "name", "amount", "categoryName", "centerName", "createDate",
                "updateDate"}) {
            for (String order : new String[] {"asc", "desc"}) {
                mvc.perform(auth(get("/api/v1/parts-stocks").param("sort", sort).param("order", order)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.meta.total", greaterThanOrEqualTo(20)));
            }
        }
    }

    /** 製品コードは、製品在庫では並び替えに使え、部品在庫では 400 になること。 */
    @Test
    void productCodeSortIsOnlyForProducts() throws Exception {
        mvc.perform(auth(get("/api/v1/products-stocks").param("sort", "productCode").param("order", "asc")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].productCode").value("PRD-000001"));
        mvc.perform(auth(get("/api/v1/parts-stocks").param("sort", "productCode")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BUSINESS_ERROR"));
    }

    /** 検索条件（センター、名称、個数の範囲）が組み合わせて効くこと。 */
    @Test
    void partsListFiltersByCenterNameAndAmountRange() throws Exception {
        mvc.perform(auth(get("/api/v1/parts-stocks").param("centerId", String.valueOf(tokyoCenterId))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.meta.total", greaterThanOrEqualTo(10)));
        mvc.perform(auth(get("/api/v1/parts-stocks").param("name", "esc").param("amountMin", "100")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.meta.total").value(1));
        mvc.perform(auth(get("/api/v1/parts-stocks").param("amountMin", "10").param("amountMax", "5")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BUSINESS_ERROR"));
    }

    /** ページ番号が総ページ数を超えても 200 で空配列を返すこと。 */
    @Test
    void listBeyondLastPageReturnsEmptyData() throws Exception {
        mvc.perform(auth(get("/api/v1/products-stocks").param("page", "99").param("size", "10")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.meta.total", greaterThanOrEqualTo(12)));
    }

    // ---- カテゴリと部品在庫の一連の流れ ----

    /** カテゴリの作成から、部品在庫の登録、検索、更新、削除、カテゴリの削除までを通して確認すること。 */
    @Test
    void categoryAndPartsStockLifecycle() throws Exception {
        String categoryName = "T" + suffix();
        String stockName = "部品" + suffix();
        int categoryId = createCategory("part", categoryName);

        // 同じ種別の同名は 409。別の種別なら作れる
        send(post("/api/v1/categories"), categoryJson("part", categoryName)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
        int productCategoryId = createCategory("product", categoryName);

        // 部品在庫を登録（カテゴリ名とセンター名が結合される）
        int stockId = json(send(post("/api/v1/parts-stocks"), partJson(categoryId, stockName, tokyoCenterId, 30))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryName").value(categoryName))
                .andExpect(jsonPath("$.centerName").value("東京センター"))
                .andExpect(jsonPath("$.amount").value(30))).get("stockId").asInt();

        // 同じセンターの同名は 409
        send(post("/api/v1/parts-stocks"), partJson(categoryId, stockName, tokyoCenterId, 1))
                .andExpect(status().isConflict());

        // 製品用のカテゴリは部品在庫に使えない
        send(post("/api/v1/parts-stocks"), partJson(productCategoryId, "別の部品" + suffix(), tokyoCenterId, 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BUSINESS_ERROR"));
        // 存在しないカテゴリ、センター
        send(post("/api/v1/parts-stocks"), partJson(999999, "別の部品" + suffix(), tokyoCenterId, 1))
                .andExpect(status().isBadRequest());
        send(post("/api/v1/parts-stocks"), partJson(categoryId, "別の部品" + suffix(), 999999, 1))
                .andExpect(status().isBadRequest());

        // 検索
        mvc.perform(auth(get("/api/v1/parts-stocks").param("categoryId", String.valueOf(categoryId))
                        .param("name", stockName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").value(1))
                .andExpect(jsonPath("$.data[0].stockId").value(stockId));

        // 更新
        send(put("/api/v1/parts-stocks/" + stockId), partJson(categoryId, stockName, tokyoCenterId, 45))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(45));
        send(put("/api/v1/parts-stocks/999999"), partJson(categoryId, stockName, tokyoCenterId, 1))
                .andExpect(status().isNotFound());

        // 使用中のカテゴリは削除・種別変更ができない
        mvc.perform(auth(delete("/api/v1/categories/" + categoryId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BUSINESS_ERROR"));
        send(put("/api/v1/categories/" + categoryId), categoryJson("product", categoryName))
                .andExpect(status().isBadRequest());

        // 在庫を削除すると、検索に出なくなり、再削除は 404
        mvc.perform(auth(delete("/api/v1/parts-stocks/" + stockId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result").value("success"));
        mvc.perform(auth(delete("/api/v1/parts-stocks/" + stockId))).andExpect(status().isNotFound());
        mvc.perform(auth(get("/api/v1/parts-stocks").param("name", stockName)))
                .andExpect(jsonPath("$.meta.total").value(0));

        // 使われなくなったカテゴリは削除でき、再削除は 404。削除したカテゴリは在庫に使えない
        mvc.perform(auth(delete("/api/v1/categories/" + categoryId))).andExpect(status().isOk());
        mvc.perform(auth(delete("/api/v1/categories/" + categoryId))).andExpect(status().isNotFound());
        send(post("/api/v1/parts-stocks"), partJson(categoryId, "削除後" + suffix(), tokyoCenterId, 1))
                .andExpect(status().isBadRequest());
    }

    /** 削除した在庫と同じ名称は、同じセンターに再登録できること（削除済みは重複にしない）。 */
    @Test
    void deletedStockNameCanBeReused() throws Exception {
        int categoryId = createCategory("part", "T" + suffix());
        String name = "再利用" + suffix();
        int first = json(send(post("/api/v1/parts-stocks"), partJson(categoryId, name, tokyoCenterId, 1))
                .andExpect(status().isOk())).get("stockId").asInt();
        mvc.perform(auth(delete("/api/v1/parts-stocks/" + first))).andExpect(status().isOk());

        send(post("/api/v1/parts-stocks"), partJson(categoryId, name, tokyoCenterId, 2))
                .andExpect(status().isOk());
    }

    /** 別のセンターには、同じ名称の在庫を登録できること（センターごとに別の管理）。 */
    @Test
    void sameNameIsAllowedInAnotherCenter() throws Exception {
        int categoryId = createCategory("part", "T" + suffix());
        int osakaId = json(mvc.perform(auth(get("/api/v1/centers").param("centerName", "大阪"))))
                .get("data").get(0).get("centerId").asInt();
        String name = "共通名" + suffix();

        send(post("/api/v1/parts-stocks"), partJson(categoryId, name, tokyoCenterId, 1)).andExpect(status().isOk());
        send(post("/api/v1/parts-stocks"), partJson(categoryId, name, osakaId, 1)).andExpect(status().isOk());
    }

    // ---- 製品在庫 ----

    /** 製品在庫の登録、製品コードでの検索、重複（削除後も再利用不可）、更新、削除を通して確認すること。 */
    @Test
    void productsStockLifecycleWithProductCode() throws Exception {
        String code = "IT-" + suffix();
        String name = "製品" + suffix();

        int stockId = json(send(post("/api/v1/products-stocks"),
                productJson(code, SEED_PRODUCT_CATEGORY_ID, name, tokyoCenterId, 7))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productCode").value(code))
                .andExpect(jsonPath("$.categoryName").value("災害用ドローン"))
                .andExpect(jsonPath("$.centerName").value("東京センター"))).get("stockId").asInt();

        // 製品コードの重複は 409、部品用のカテゴリは 400
        send(post("/api/v1/products-stocks"),
                productJson(code, SEED_PRODUCT_CATEGORY_ID, "別名" + suffix(), tokyoCenterId, 1))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
        send(post("/api/v1/products-stocks"), productJson("IT-" + suffix(), 1, "別名" + suffix(), tokyoCenterId, 1))
                .andExpect(status().isBadRequest());
        // 使えない文字の製品コードは 400（入力検証）
        send(post("/api/v1/products-stocks"),
                productJson("製品コード", SEED_PRODUCT_CATEGORY_ID, "別名" + suffix(), tokyoCenterId, 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        // 製品コードでの検索
        mvc.perform(auth(get("/api/v1/products-stocks").param("productCode", code.toLowerCase())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").value(1))
                .andExpect(jsonPath("$.data[0].stockId").value(stockId));

        // 更新（製品コードは自分自身なら変えなくてよい）
        send(put("/api/v1/products-stocks/" + stockId),
                productJson(code, SEED_PRODUCT_CATEGORY_ID, name, tokyoCenterId, 8))
                .andExpect(status().isOk()).andExpect(jsonPath("$.amount").value(8));

        // 削除すると検索に出ないが、製品コードは削除後も再利用できない
        mvc.perform(auth(delete("/api/v1/products-stocks/" + stockId))).andExpect(status().isOk());
        mvc.perform(auth(get("/api/v1/products-stocks").param("productCode", code)))
                .andExpect(jsonPath("$.meta.total").value(0));
        send(post("/api/v1/products-stocks"),
                productJson(code, SEED_PRODUCT_CATEGORY_ID, "再登録" + suffix(), tokyoCenterId, 1))
                .andExpect(status().isConflict());
        mvc.perform(auth(delete("/api/v1/products-stocks/" + stockId))).andExpect(status().isNotFound());
    }

    /** 入力検証の違反は、共通のエラー形式（VALIDATION_ERROR）で返ること。 */
    @Test
    void invalidBodiesAreRejectedWithValidationError() throws Exception {
        send(post("/api/v1/categories"), categoryJson("part", "<b>")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        send(post("/api/v1/parts-stocks"), partJson(1, "負の数量", tokyoCenterId, -1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        send(post("/api/v1/parts-stocks"), "{}").andExpect(status().isBadRequest());
    }
}
