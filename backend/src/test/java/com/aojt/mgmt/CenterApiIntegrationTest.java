package com.aojt.mgmt;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

/**
 * パッケージを横断する結合テスト。登録からトークンを取得し、在庫センター API を実際のセキュリティ設定と DB（H2）で検証する。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CenterApiIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    String accessToken;
    String refreshToken;
    int userId;

    @BeforeEach
    void registerUser() throws Exception {
        String body = """
                {"firstname":"Taro","lastname":"Test","email":"%s@example.com","password":"password123"}
                """.formatted(UUID.randomUUID());
        String json = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(json);
        accessToken = node.get("accessToken").asText();
        refreshToken = node.get("refreshToken").asText();
        userId = node.get("user").get("id").asInt();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private static String centerJson(String name, int max, int current) {
        return """
                {"centerName":"%s","postCode":"530-0001","address":"大阪府大阪市北区梅田1-1-1",
                 "phoneNumber":"06-1234-5678","managerName":"田中一郎","operationalStatus":0,
                 "maxStorageCapacity":%d,"currentStorageCapacity":%d,"notes":""}
                """.formatted(name, max, current);
    }

    @Test
    void listRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/centers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_ERROR"));
    }

    @Test
    void refreshTokenCannotAccessApi() throws Exception {
        mvc.perform(get("/api/v1/centers").header(HttpHeaders.AUTHORIZATION, bearer(refreshToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void listReturnsSeededCentersWithDefaultsAndMeta() throws Exception {
        mvc.perform(get("/api/v1/centers").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.size").value(20))
                .andExpect(jsonPath("$.meta.total").isNumber())
                .andExpect(jsonPath("$.data[0].centerId").isNumber())
                .andExpect(jsonPath("$.data[0].deleteFlag").doesNotExist());
    }

    @Test
    void listFiltersByNameAddressAndStatus() throws Exception {
        mvc.perform(get("/api/v1/centers").param("centerName", "東京")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].centerName").value("東京センター"));

        mvc.perform(get("/api/v1/centers").param("address", "大阪府")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].centerName").value("大阪センター"));

        mvc.perform(get("/api/v1/centers").param("operationalStatus", "0")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").value(3))
                .andExpect(jsonPath("$.data[*].operationalStatus", org.hamcrest.Matchers.everyItem(is(0))));
    }

    @Test
    void listFiltersStoppedCenters() throws Exception {
        String name = "S" + UUID.randomUUID().toString().substring(0, 8);
        String created = mvc.perform(post("/api/v1/centers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(centerJson(name, 100, 0).replace("\"operationalStatus\":0", "\"operationalStatus\":1")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int id = objectMapper.readTree(created).get("centerId").asInt();

        mvc.perform(get("/api/v1/centers").param("operationalStatus", "1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").value(1))
                .andExpect(jsonPath("$.data[0].centerName").value(name));

        mvc.perform(delete("/api/v1/centers/" + id).header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk());
    }

    @Test
    void likeWildcardsInSearchAreTreatedLiterally() throws Exception {
        mvc.perform(get("/api/v1/centers").param("centerName", "%")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").value(0));
    }

    @Test
    void listSupportsSortingAndPaging() throws Exception {
        mvc.perform(get("/api/v1/centers").param("sort", "maxStorageCapacity").param("order", "asc")
                        .param("size", "2").param("page", "1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].maxStorageCapacity").value(6000))
                .andExpect(jsonPath("$.meta.size").value(2));
    }

    @Test
    void listHandlesPageOffsetsBeyondJpaIntegerLimit() throws Exception {
        int[] pages = {21_474_837, 21_474_838, Integer.MAX_VALUE};
        int[] sizes = {100, 100, 1};
        for (int i = 0; i < pages.length; i++) {
            mvc.perform(get("/api/v1/centers").param("page", Integer.toString(pages[i]))
                            .param("size", Integer.toString(sizes[i]))
                            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(0)))
                    .andExpect(jsonPath("$.meta.total").value(3))
                    .andExpect(jsonPath("$.meta.page").value(pages[i]));
        }
    }

    @Test
    void listKeepsEmptyResultMetaAtZeroPages() throws Exception {
        mvc.perform(get("/api/v1/centers").param("centerName", "存在しない名前")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.meta.total").value(0))
                .andExpect(jsonPath("$.meta.totalPages").value(0));
    }

    @Test
    void listNormalizesSearchWhitespaceBeforeValidation() throws Exception {
        mvc.perform(get("/api/v1/centers").param("centerName", "  " + "あ".repeat(20))
                        .param("address", "  東京都  ")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/centers").param("centerName", "  " + "あ".repeat(21))
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void listSortsEverySupportedFieldInBothDirections() throws Exception {
        String[] fields = {"centerId", "centerName", "address", "operationalStatus",
                "maxStorageCapacity", "currentStorageCapacity", "createDate", "updateDate"};
        for (String field : fields) {
            for (String order : new String[] {"asc", "desc"}) {
                String body = mvc.perform(get("/api/v1/centers").param("sort", field).param("order", order)
                                .param("size", "100")
                                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data", hasSize(3)))
                        .andReturn().getResponse().getContentAsString();
                var data = objectMapper.readTree(body).get("data");
                for (int i = 1; i < data.size(); i++) {
                    var previous = data.get(i - 1);
                    var current = data.get(i);
                    int comparison = compareSortValues(previous, current, field);
                    if (comparison == 0) {
                        assertTrue(previous.get("centerId").asInt() < current.get("centerId").asInt());
                    } else if ("asc".equals(order)) {
                        assertTrue(comparison < 0, field + " should be ascending");
                    } else {
                        assertTrue(comparison > 0, field + " should be descending");
                    }
                }
            }
        }
    }

    @Test
    void listUsesStableTieBreakerAcrossPages() throws Exception {
        String first = mvc.perform(get("/api/v1/centers").param("sort", "operationalStatus")
                        .param("order", "asc").param("page", "1").param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String second = mvc.perform(get("/api/v1/centers").param("sort", "operationalStatus")
                        .param("order", "asc").param("page", "2").param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertEquals(1, objectMapper.readTree(first).get("data").get(0).get("centerId").asInt());
        assertEquals(2, objectMapper.readTree(first).get("data").get(1).get("centerId").asInt());
        assertEquals(3, objectMapper.readTree(second).get("data").get(0).get("centerId").asInt());
    }

    @Test
    void responseKeepsDateFormatAndNullableCapacities() throws Exception {
        String name = "N" + UUID.randomUUID().toString().substring(0, 8);
        String request = centerJson(name, 0, 0)
                .replace("\"maxStorageCapacity\":0", "\"maxStorageCapacity\":null")
                .replace("\"currentStorageCapacity\":0", "\"currentStorageCapacity\":null");
        String body = mvc.perform(post("/api/v1/centers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode created = objectMapper.readTree(body);
        assertTrue(created.get("maxStorageCapacity").isNull());
        assertTrue(created.get("currentStorageCapacity").isNull());
        assertTrue(created.get("createDate").asText()
                .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?"));

        mvc.perform(delete("/api/v1/centers/" + created.get("centerId").asInt())
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk());
    }

    private static int compareSortValues(com.fasterxml.jackson.databind.JsonNode left,
            com.fasterxml.jackson.databind.JsonNode right, String field) {
        if (left.get(field).isNumber()) {
            return Long.compare(left.get(field).asLong(), right.get(field).asLong());
        }
        return left.get(field).asText().compareTo(right.get(field).asText());
    }

    @Test
    void listRejectsInvalidParameters() throws Exception {
        String[][] invalid = {
                {"page", "0"}, {"size", "101"}, {"order", "up"}, {"sort", "password"},
                {"operationalStatus", "2"}, {"operationalStatus", "abc"}
        };
        for (String[] p : invalid) {
            mvc.perform(get("/api/v1/centers").param(p[0], p[1])
                            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }
    }

    @Test
    void createUpdateAndLogicallyDelete() throws Exception {
        String name = "T" + UUID.randomUUID().toString().substring(0, 8);
        String created = mvc.perform(post("/api/v1/centers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON).content(centerJson(name, 800, 0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.centerName").value(name))
                .andExpect(jsonPath("$.createDate").exists())
                .andReturn().getResponse().getContentAsString();
        int id = objectMapper.readTree(created).get("centerId").asInt();

        mvc.perform(post("/api/v1/centers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON).content(centerJson(name, 800, 0)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));

        mvc.perform(put("/api/v1/centers/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON).content(centerJson(name, 900, 10)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxStorageCapacity").value(900));

        mvc.perform(delete("/api/v1/centers/" + id).header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("success"));

        mvc.perform(get("/api/v1/centers").param("centerName", name)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(jsonPath("$.meta.total").value(0));

        mvc.perform(delete("/api/v1/centers/" + id).header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createValidatesInput() throws Exception {
        String[] bodies = {
                centerJson("", 100, 0),
                centerJson("a".repeat(21), 100, 0),
                centerJson("X<script>", 100, 0),
                centerJson("Over", 100, 200),
                centerJson("Bad", 100, 0).replace("530-0001", "5300001"),
                centerJson("Bad", 100, 0).replace("06-1234-5678", "abc"),
                centerJson("Bad", 100, 0).replace("\"operationalStatus\":0", "\"operationalStatus\":5"),
                centerJson("Bad", 100, 0).replace("\"phoneNumber\":\"06-1234-5678\",", ""),
                centerJson("Bad", 100, 0).replace("\"managerName\":\"田中一郎\",", ""),
                centerJson("Bad", 100, 0).replace("530-0001", "530-00011"),
                "{not json"
        };
        for (String body : bodies) {
            mvc.perform(post("/api/v1/centers")
                            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void updateUnknownCenterReturnsNotFound() throws Exception {
        mvc.perform(put("/api/v1/centers/999999")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON).content(centerJson("Nowhere", 1, 0)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void authenticateAndRefreshFlow() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstname\":\"A\",\"lastname\":\"B\",\"email\":\"" + email
                                + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.password").doesNotExist());

        mvc.perform(post("/api/v1/auth/authenticate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());

        String json = mvc.perform(post("/api/v1/auth/authenticate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String refresh = objectMapper.readTree(json).get("refreshToken").asText();
        String access = objectMapper.readTree(json).get("accessToken").asText();

        mvc.perform(post("/api/v1/auth/refresh-token").header(HttpHeaders.AUTHORIZATION, bearer(refresh)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user").doesNotExist());

        mvc.perform(post("/api/v1/auth/refresh-token").header(HttpHeaders.AUTHORIZATION, bearer(access)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userEndpointHidesPasswordAndRestrictsToSelf() throws Exception {
        mvc.perform(get("/api/v1/users/" + userId).header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.role").value("USER"));

        mvc.perform(get("/api/v1/users/" + (userId + 1000)).header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isNotFound());
    }
}
