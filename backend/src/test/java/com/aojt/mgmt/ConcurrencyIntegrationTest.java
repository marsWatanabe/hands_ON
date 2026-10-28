package com.aojt.mgmt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * 同時リクエストでも、重複と矛盾が生じないことの結合テスト。実際のセキュリティ設定と DB（H2）で、複数のスレッドから
 * 同時に API を呼ぶ。実行順は毎回変わるため、「何番目が成功するか」ではなく、どの順序でも成り立つ不変条件
 * （成功は 1 件だけ、両方が成功することは無い）を検証する。
 *
 * <ul>
 *   <li>同じ名称（カテゴリ名、在庫の名称）の同時登録は、DB の一意制約により 1 件だけ成功し、残りは 409 になる。
 *   <li>カテゴリの削除と、そのカテゴリを使う在庫の登録は、行ロックで直列になり、両方が成功することは無い。
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConcurrencyIntegrationTest {

    /** 同時に送るリクエストの数。 */
    private static final int THREADS = 6;

    /** 競合の再現を繰り返す回数。 */
    private static final int ROUNDS = 8;

    private static final int WAIT_SECONDS = 30;

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    String accessToken;
    int tokyoCenterId;

    @BeforeEach
    void registerUserAndFindCenter() throws Exception {
        String body = """
                {"firstname":"Taro","lastname":"Race","email":"%s@example.com","password":"password123"}
                """.formatted(UUID.randomUUID());
        accessToken = read(mvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .get("accessToken").asText();
        tokyoCenterId = read(mvc.perform(get("/api/v1/centers").param("centerName", "東京")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andReturn().getResponse().getContentAsString()).get("data").get(0).get("centerId").asInt();
    }

    private JsonNode read(String json) throws Exception {
        return objectMapper.readTree(json);
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /** リクエストを送り、HTTP ステータスを返す。{@code body} が null なら本文なし。 */
    private int send(MockHttpServletRequestBuilder builder, String body) throws Exception {
        builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mvc.perform(builder).andReturn().getResponse().getStatus();
    }

    private JsonNode getJson(String path, String param, Object value) throws Exception {
        return read(mvc.perform(get(path).param(param, String.valueOf(value))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    /** すべてのタスクを、できるだけ同時に開始して、各タスクの結果を返す。 */
    private static List<Integer> runConcurrently(List<Callable<Integer>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        try {
            CountDownLatch ready = new CountDownLatch(tasks.size());
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Integer>> futures = new ArrayList<>();
            for (Callable<Integer> task : tasks) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    return task.call();
                }));
            }
            assertTrue(ready.await(WAIT_SECONDS, TimeUnit.SECONDS));
            start.countDown();
            List<Integer> results = new ArrayList<>();
            for (Future<Integer> future : futures) {
                results.add(future.get(WAIT_SECONDS, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    private static long count(List<Integer> statuses, int expected) {
        return statuses.stream().filter(s -> s == expected).count();
    }

    private static String categoryJson(String type, String name) {
        return """
                {"categoryType":"%s","categoryName":"%s","description":"同時実行テスト"}""".formatted(type, name);
    }

    private static String partJson(int categoryId, String name, int centerId) {
        return """
                {"categoryId":%d,"name":"%s","centerId":%d,"description":"同時実行テスト","amount":1}"""
                .formatted(categoryId, name, centerId);
    }

    private static String productJson(String code, int categoryId, String name, int centerId) {
        return """
                {"productCode":"%s","categoryId":%d,"name":"%s","centerId":%d,"description":"同時実行テスト","amount":1}"""
                .formatted(code, categoryId, name, centerId);
    }

    private int createCategory(String type, String name) throws Exception {
        MockHttpServletRequestBuilder request = post("/api/v1/categories")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON).content(categoryJson(type, name));
        return read(mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .get("categoryId").asInt();
    }

    /** 同じカテゴリ名を同時に登録すると、1 件だけ成功し、残りは 409 になること。 */
    @Test
    void sameCategoryNameCreatedConcurrentlyYieldsExactlyOne() throws Exception {
        String name = "CC-" + suffix();
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            tasks.add(() -> send(post("/api/v1/categories"), categoryJson("part", name)));
        }

        List<Integer> statuses = runConcurrently(tasks);

        assertEquals(1, count(statuses, 200), statuses.toString());
        assertEquals(THREADS - 1, count(statuses, 409), statuses.toString());
        assertEquals(1, getJson("/api/v1/categories", "categoryName", name).get("meta").get("total").asInt());
    }

    /** 同じセンターに同じ名称の部品在庫を同時に登録すると、1 件だけ成功し、残りは 409 になること。 */
    @Test
    void samePartNameCreatedConcurrentlyYieldsExactlyOne() throws Exception {
        int categoryId = createCategory("part", "CP-" + suffix());
        String name = "CPS-" + suffix();
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            tasks.add(() -> send(post("/api/v1/parts-stocks"), partJson(categoryId, name, tokyoCenterId)));
        }

        List<Integer> statuses = runConcurrently(tasks);

        assertEquals(1, count(statuses, 200), statuses.toString());
        assertEquals(THREADS - 1, count(statuses, 409), statuses.toString());
        assertEquals(1, getJson("/api/v1/parts-stocks", "name", name).get("meta").get("total").asInt());
    }

    /** 同じセンターに同じ名称の製品在庫を（製品コードを変えて）同時に登録すると、1 件だけ成功すること。 */
    @Test
    void sameProductNameCreatedConcurrentlyYieldsExactlyOne() throws Exception {
        int categoryId = createCategory("product", "CQ-" + suffix());
        String name = "CQS-" + suffix();
        String codePrefix = "R" + suffix();
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            String code = codePrefix + "-" + i;
            tasks.add(() -> send(post("/api/v1/products-stocks"), productJson(code, categoryId, name, tokyoCenterId)));
        }

        List<Integer> statuses = runConcurrently(tasks);

        assertEquals(1, count(statuses, 200), statuses.toString());
        assertEquals(THREADS - 1, count(statuses, 409), statuses.toString());
        assertEquals(1, getJson("/api/v1/products-stocks", "name", name).get("meta").get("total").asInt());
    }

    /** 同じ製品コードを同時に登録すると、1 件だけ成功し、残りは 409 になること（DB の一意制約）。 */
    @Test
    void sameProductCodeCreatedConcurrentlyYieldsExactlyOne() throws Exception {
        int categoryId = createCategory("product", "CR-" + suffix());
        String code = "R" + suffix();
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            String name = "CRS-" + suffix();
            tasks.add(() -> send(post("/api/v1/products-stocks"), productJson(code, categoryId, name, tokyoCenterId)));
        }

        List<Integer> statuses = runConcurrently(tasks);

        assertEquals(1, count(statuses, 200), statuses.toString());
        assertEquals(THREADS - 1, count(statuses, 409), statuses.toString());
        assertEquals(1, getJson("/api/v1/products-stocks", "productCode", code).get("meta").get("total").asInt());
    }

    /**
     * カテゴリの削除と、そのカテゴリを使う部品在庫の登録を同時に行うと、必ずどちらか一方だけが成功すること。
     * 削除が先なら在庫の登録が 400（カテゴリが無い）、登録が先なら削除が 400（使用中）になる。
     * 両方が成功すると「削除済みのカテゴリを参照する在庫」ができてしまう。
     */
    @Test
    void categoryDeleteAndStockCreationNeverSucceedTogether() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            int categoryId = createCategory("part", "DR-" + suffix());
            String stockName = "DRS-" + suffix();

            List<Integer> statuses = runConcurrently(List.of(
                    () -> send(delete("/api/v1/categories/" + categoryId), null),
                    () -> send(post("/api/v1/parts-stocks"), partJson(categoryId, stockName, tokyoCenterId))));

            int deleteStatus = statuses.get(0);
            int createStatus = statuses.get(1);
            String message = "round " + round + ": delete=" + deleteStatus + ", create=" + createStatus;
            boolean deletedFirst = deleteStatus == 200 && createStatus == 400;
            boolean createdFirst = deleteStatus == 400 && createStatus == 200;
            assertTrue(deletedFirst || createdFirst, message);

            int stocks = getJson("/api/v1/parts-stocks", "categoryId", categoryId).get("meta").get("total").asInt();
            assertEquals(createdFirst ? 1 : 0, stocks, message);
        }
    }
}
