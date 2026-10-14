package com.aojt.mgmt.controller.center;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aojt.mgmt.config.SecurityConfig;
import com.aojt.mgmt.config.SecurityErrorHandlers;
import com.aojt.mgmt.dto.center.CenterRequest;
import com.aojt.mgmt.dto.center.CenterResponse;
import com.aojt.mgmt.dto.center.CenterSearchRequest;
import com.aojt.mgmt.dto.common.PageResponse;
import com.aojt.mgmt.exception.BusinessException;
import com.aojt.mgmt.exception.ConflictException;
import com.aojt.mgmt.exception.NotFoundException;
import com.aojt.mgmt.service.center.CenterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * {@link CenterController} のテスト。サービスをモックにし、認証・認可、入力検証、エラー応答の形式、
 * サービスへの委譲を確認する。
 */
@WebMvcTest(CenterController.class)
@Import({SecurityConfig.class, SecurityErrorHandlers.class})
class CenterControllerTest {

    private static final String URL = "/api/v1/centers";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    CenterService service;

    @MockitoBean
    JwtDecoder jwtDecoder;

    private CenterResponse response;

    @BeforeEach
    void setUp() {
        response = new CenterResponse(1, "大阪センター", "530-0001", "大阪府大阪市北区梅田1-1-1", "06-1234-5678",
                "田中一郎", 0, 800, 0, "", null, null);
    }

    private Map<String, Object> validBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("centerName", "大阪センター");
        body.put("postCode", "530-0001");
        body.put("address", "大阪府大阪市北区梅田1-1-1");
        body.put("phoneNumber", "06-1234-5678");
        body.put("managerName", "田中一郎");
        body.put("operationalStatus", 0);
        body.put("maxStorageCapacity", 800);
        body.put("currentStorageCapacity", 0);
        body.put("notes", "");
        return body;
    }

    private ResultActions postJson(String json) throws Exception {
        return mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions postBody(Map<String, Object> body) throws Exception {
        return postJson(objectMapper.writeValueAsString(body));
    }

    private void assertCreateRejected(String field, Object value) throws Exception {
        Map<String, Object> body = validBody();
        if (value == null) {
            body.remove(field);
        } else {
            body.put(field, value);
        }
        postBody(body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message", containsString(field)));
    }

    // ---- 認証・認可 ----

    /** トークンが無ければ、共通形式の 401（AUTH_ERROR）を返し、サービスを呼ばないこと。 */
    @Test
    void list_returns401WithoutAuthentication() throws Exception {
        mvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_ERROR"))
                .andExpect(jsonPath("$.error.timestamp").exists());
        verifyNoInteractions(service);
    }

    /** USER・ADMIN 以外のロールは、403（FORBIDDEN）を返すこと。 */
    @Test
    @WithMockUser(roles = "GUEST")
    void list_returns403ForRoleWithoutPermission() throws Exception {
        mvc.perform(get(URL))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verifyNoInteractions(service);
    }

    /** ADMIN も一覧を取得できること。 */
    @Test
    @WithMockUser(roles = "ADMIN")
    void list_allowsAdmin() throws Exception {
        when(service.search(any())).thenReturn(new PageResponse<>(List.of(), new PageResponse.Meta(0, 1, 20, 0)));

        mvc.perform(get(URL)).andExpect(status().isOk());
    }

    /** 作成・更新・削除も、認証が無ければ 401 になること。 */
    @Test
    void writeOperations_return401WithoutAuthentication() throws Exception {
        postBody(validBody()).andExpect(status().isUnauthorized());
        mvc.perform(put(URL + "/1").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validBody()))).andExpect(status().isUnauthorized());
        mvc.perform(delete(URL + "/1")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    // ---- 一覧 ----

    /** パラメータが無ければ既定値（1 ページ目、20 件、centerId の降順）でサービスを呼び、data と meta を返すこと。 */
    @Test
    @WithMockUser(roles = "USER")
    void list_callsServiceWithDefaultsAndReturnsPage() throws Exception {
        when(service.search(any())).thenReturn(
                new PageResponse<>(List.of(response), new PageResponse.Meta(1, 1, 20, 1)));

        mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].centerName").value("大阪センター"))
                .andExpect(jsonPath("$.data[0].deleteFlag").doesNotExist())
                .andExpect(jsonPath("$.meta.total").value(1))
                .andExpect(jsonPath("$.meta.totalPages").value(1));

        ArgumentCaptor<CenterSearchRequest> captor = ArgumentCaptor.forClass(CenterSearchRequest.class);
        verify(service).search(captor.capture());
        assertEquals(1, captor.getValue().getPage());
        assertEquals(20, captor.getValue().getSize());
        assertEquals("centerId", captor.getValue().getSort());
        assertEquals("desc", captor.getValue().getOrder());
    }

    /** クエリパラメータをそのままサービスに渡すこと。 */
    @Test
    @WithMockUser(roles = "USER")
    void list_passesQueryParametersToService() throws Exception {
        when(service.search(any())).thenReturn(new PageResponse<>(List.of(), new PageResponse.Meta(0, 2, 5, 0)));

        mvc.perform(get(URL).param("centerName", "東京").param("address", "千代田")
                .param("operationalStatus", "1").param("page", "2").param("size", "5")
                .param("sort", "centerName").param("order", "asc"))
                .andExpect(status().isOk());

        ArgumentCaptor<CenterSearchRequest> captor = ArgumentCaptor.forClass(CenterSearchRequest.class);
        verify(service).search(captor.capture());
        CenterSearchRequest r = captor.getValue();
        assertEquals("東京", r.getCenterName());
        assertEquals("千代田", r.getAddress());
        assertEquals(1, r.getOperationalStatus());
        assertEquals(2, r.getPage());
        assertEquals(5, r.getSize());
        assertEquals("centerName", r.getSort());
        assertEquals("asc", r.getOrder());
    }

    /** 範囲外・不正なパラメータは、サービスを呼ばずに 400（VALIDATION_ERROR）にすること（境界値を含む）。 */
    @Test
    @WithMockUser(roles = "USER")
    void list_rejectsInvalidParameters() throws Exception {
        String[][] invalid = {
                {"page", "0"}, {"page", "-1"}, {"size", "0"}, {"size", "101"}, {"order", "up"},
                {"sort", "password"}, {"operationalStatus", "-1"}, {"operationalStatus", "2"},
                {"operationalStatus", "abc"}, {"centerName", "あ".repeat(21)}, {"address", "あ".repeat(101)}
        };
        for (String[] p : invalid) {
            mvc.perform(get(URL).param(p[0], p[1]))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.error.message", containsString(p[0])));
        }
        verifyNoInteractions(service);
    }

    /** 上限・下限ちょうどの値は受け付けること。 */
    @Test
    @WithMockUser(roles = "USER")
    void list_acceptsBoundaryValues() throws Exception {
        when(service.search(any())).thenReturn(new PageResponse<>(List.of(), new PageResponse.Meta(0, 1, 100, 0)));

        mvc.perform(get(URL).param("page", "1").param("size", "100").param("operationalStatus", "0")
                .param("centerName", "あ".repeat(20)).param("address", "あ".repeat(100)).param("order", "ASC"))
                .andExpect(status().isOk());
    }

    /** 業務例外（ソート項目の不正など）は、400（BUSINESS_ERROR）にすること。 */
    @Test
    @WithMockUser(roles = "USER")
    void list_mapsBusinessExceptionTo400() throws Exception {
        when(service.search(any())).thenThrow(new BusinessException("並び替え項目が不正です。"));

        mvc.perform(get(URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BUSINESS_ERROR"))
                .andExpect(jsonPath("$.error.message").value("並び替え項目が不正です。"));
    }

    // ---- 作成 ----

    /** 正しい入力ならサービスに委譲し、作成結果を 200 で返すこと。 */
    @Test
    @WithMockUser(roles = "USER")
    void create_returnsCreatedCenter() throws Exception {
        when(service.create(any(CenterRequest.class))).thenReturn(response);

        postBody(validBody())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.centerId").value(1))
                .andExpect(jsonPath("$.centerName").value("大阪センター"));

        ArgumentCaptor<CenterRequest> captor = ArgumentCaptor.forClass(CenterRequest.class);
        verify(service).create(captor.capture());
        assertEquals("530-0001", captor.getValue().postCode());
    }

    /** 容量が未設定でも、境界値（名称 20 文字、稼働状況 1、容量 0）でも作成できること。 */
    @Test
    @WithMockUser(roles = "USER")
    void create_acceptsBoundaryAndOptionalValues() throws Exception {
        when(service.create(any(CenterRequest.class))).thenReturn(response);
        Map<String, Object> body = validBody();
        body.put("centerName", "あ".repeat(20));
        body.put("operationalStatus", 1);
        body.put("maxStorageCapacity", 0);
        body.put("currentStorageCapacity", 0);
        postBody(body).andExpect(status().isOk());

        Map<String, Object> noCapacity = validBody();
        noCapacity.remove("maxStorageCapacity");
        noCapacity.remove("currentStorageCapacity");
        postBody(noCapacity).andExpect(status().isOk());
    }

    /** 必須項目の不足、桁数・形式・範囲の違反は 400（VALIDATION_ERROR）にし、サービスを呼ばないこと。 */
    @Test
    @WithMockUser(roles = "USER")
    void create_rejectsInvalidInput() throws Exception {
        assertCreateRejected("centerName", "");
        assertCreateRejected("centerName", "   ");
        assertCreateRejected("centerName", null);
        assertCreateRejected("centerName", "あ".repeat(21));
        assertCreateRejected("centerName", "<script>");
        assertCreateRejected("postCode", null);
        assertCreateRejected("postCode", "5300001");
        assertCreateRejected("postCode", "530-00011");
        assertCreateRejected("address", null);
        assertCreateRejected("address", "あ".repeat(256));
        assertCreateRejected("phoneNumber", null);
        assertCreateRejected("phoneNumber", "abc");
        assertCreateRejected("managerName", null);
        assertCreateRejected("managerName", "あ".repeat(101));
        assertCreateRejected("operationalStatus", null);
        assertCreateRejected("operationalStatus", 2);
        assertCreateRejected("operationalStatus", -1);
        assertCreateRejected("maxStorageCapacity", -1);
        assertCreateRejected("currentStorageCapacity", -1);
        assertCreateRejected("notes", "あ".repeat(256));
        verifyNoInteractions(service);
    }

    /** 壊れた JSON は 400 にし、内部の解析エラーの詳細を返さないこと。 */
    @Test
    @WithMockUser(roles = "USER")
    void create_rejectsMalformedJsonWithoutLeakingDetails() throws Exception {
        postJson("{not json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(content().string(not(containsString("JsonParseException"))));
    }

    /** センター名の重複は、409（CONFLICT）にすること。 */
    @Test
    @WithMockUser(roles = "USER")
    void create_mapsConflictTo409() throws Exception {
        when(service.create(any(CenterRequest.class))).thenThrow(new ConflictException("重複しています。"));

        postBody(validBody())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                .andExpect(jsonPath("$.error.message").value("重複しています。"));
    }

    /** DB の制約違反は、詳細を返さずに 409（CONFLICT）にすること。 */
    @Test
    @WithMockUser(roles = "USER")
    void create_mapsDatabaseConstraintViolationTo409WithoutDetails() throws Exception {
        when(service.create(any(CenterRequest.class)))
                .thenThrow(new DataIntegrityViolationException("could not execute statement uk_center_name"));

        postBody(validBody())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                .andExpect(content().string(not(containsString("uk_center_name"))));
    }

    /** 業務ルール違反（容量超過など）は、400（BUSINESS_ERROR）にすること。 */
    @Test
    @WithMockUser(roles = "USER")
    void create_mapsBusinessExceptionTo400() throws Exception {
        when(service.create(any(CenterRequest.class))).thenThrow(new BusinessException("容量超過"));

        postBody(validBody())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BUSINESS_ERROR"));
    }

    /** 想定外の例外は、500（INTERNAL_ERROR）にし、例外のメッセージをクライアントに返さないこと。 */
    @Test
    @WithMockUser(roles = "USER")
    void create_mapsUnexpectedExceptionTo500WithoutLeakingMessage() throws Exception {
        when(service.create(any(CenterRequest.class))).thenThrow(new IllegalStateException("secret internal detail"));

        postBody(validBody())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(not(containsString("secret internal detail"))));
    }

    // ---- 更新・削除 ----

    /** 更新はパスの ID と本文をサービスに渡し、結果を 200 で返すこと。 */
    @Test
    @WithMockUser(roles = "USER")
    void update_delegatesToServiceAndReturnsResult() throws Exception {
        when(service.update(eq(1), any(CenterRequest.class))).thenReturn(response);

        mvc.perform(put(URL + "/1").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.centerName").value("大阪センター"));
    }

    /** 存在しないセンターの更新・削除は、404（NOT_FOUND）にすること。 */
    @Test
    @WithMockUser(roles = "USER")
    void updateAndDelete_mapNotFoundTo404() throws Exception {
        when(service.update(eq(9), any(CenterRequest.class))).thenThrow(new NotFoundException("見つかりません。"));
        when(service.delete(9)).thenThrow(new NotFoundException("見つかりません。"));

        mvc.perform(put(URL + "/9").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        mvc.perform(delete(URL + "/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    /** 削除は成功時に {"result": "success"} を返すこと。 */
    @Test
    @WithMockUser(roles = "USER")
    void delete_returnsSuccessResult() throws Exception {
        when(service.delete(1)).thenReturn(Map.of("result", "success"));

        mvc.perform(delete(URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("success"));
    }

    /** ID が数値でないパスは 400 にすること。 */
    @Test
    @WithMockUser(roles = "USER")
    void delete_rejectsNonNumericId() throws Exception {
        mvc.perform(delete(URL + "/abc")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    /** 未定義のパスは 404（NOT_FOUND）、許可されないメソッドは 405 で、どちらも共通形式で返すこと。 */
    @Test
    @WithMockUser(roles = "USER")
    void unknownPathAndUnsupportedMethod_useCommonErrorFormat() throws Exception {
        mvc.perform(get(URL + "/1/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        mvc.perform(post(URL + "/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error.code").exists());
    }
}
