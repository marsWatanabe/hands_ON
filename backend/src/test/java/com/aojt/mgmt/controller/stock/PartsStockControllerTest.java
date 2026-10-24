package com.aojt.mgmt.controller.stock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aojt.mgmt.config.SecurityConfig;
import com.aojt.mgmt.config.SecurityErrorHandlers;
import com.aojt.mgmt.dto.common.PageResponse;
import com.aojt.mgmt.dto.stock.PartsStockRequest;
import com.aojt.mgmt.dto.stock.PartsStockResponse;
import com.aojt.mgmt.dto.stock.StockSearchRequest;
import com.aojt.mgmt.exception.BusinessException;
import com.aojt.mgmt.exception.ConflictException;
import com.aojt.mgmt.exception.NotFoundException;
import com.aojt.mgmt.service.stock.PartsStockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * {@link PartsStockController} のテスト。サービスをモックにし、認証・認可、クエリと本文の入力検証、
 * エラー応答の形式、サービスへの委譲を確認する。
 */
@WebMvcTest(PartsStockController.class)
@Import({SecurityConfig.class, SecurityErrorHandlers.class})
class PartsStockControllerTest {

    private static final String URL = "/api/v1/parts-stocks";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    PartsStockService service;

    @MockitoBean
    JwtDecoder jwtDecoder;

    private static PartsStockResponse response() {
        return new PartsStockResponse(1, 1, "フレーム", "カーボンフレーム", 2, "東京センター", "説明", 85, null, null);
    }

    private static Map<String, Object> validBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("categoryId", 1);
        body.put("name", "カーボンフレーム");
        body.put("centerId", 2);
        body.put("description", "説明");
        body.put("amount", 85);
        return body;
    }

    private ResultActions postBody(Map<String, Object> body) throws Exception {
        return mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
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
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(service);
    }

    // ---- 認証・認可 ----

    /** トークンが無ければ 401（AUTH_ERROR）を返し、サービスを呼ばないこと。 */
    @Test
    void list_returns401WithoutAuthentication() throws Exception {
        mvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_ERROR"));
        verifyNoInteractions(service);
    }

    /** USER・ADMIN 以外のロールは 403（FORBIDDEN）を返すこと。 */
    @Test
    @WithMockUser(roles = "GUEST")
    void list_returns403ForRoleWithoutPermission() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verifyNoInteractions(service);
    }

    // ---- 一覧 ----

    /** すべての検索条件をサービスに渡し、{@code data} と {@code meta} を返すこと。 */
    @Test
    @WithMockUser(roles = "USER")
    void list_bindsAllConditions() throws Exception {
        when(service.search(any(StockSearchRequest.class))).thenReturn(
                new PageResponse<>(List.of(response()), new PageResponse.Meta(1, 1, 20, 1)));

        mvc.perform(get(URL).param("categoryId", "1").param("centerId", "2").param("name", " フレーム ")
                        .param("amountMin", "10").param("amountMax", "100")
                        .param("sort", "amount").param("order", "desc").param("page", "3").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].categoryName").value("フレーム"))
                .andExpect(jsonPath("$.data[0].centerName").value("東京センター"))
                .andExpect(jsonPath("$.data[0].deleteFlag").doesNotExist());

        ArgumentCaptor<StockSearchRequest> captor = ArgumentCaptor.forClass(StockSearchRequest.class);
        verify(service).search(captor.capture());
        StockSearchRequest r = captor.getValue();
        assertEquals(1, r.getCategoryId());
        assertEquals(2, r.getCenterId());
        assertEquals("フレーム", r.getName());
        assertEquals(10, r.getAmountMin());
        assertEquals(100, r.getAmountMax());
        assertEquals("amount", r.getSort());
        assertEquals("desc", r.getOrder());
        assertEquals(3, r.getPage());
        assertEquals(50, r.getSize());
    }

    /** パラメータを省略すると、条件なし・カテゴリ名の昇順・1 ページ目 20 件で検索すること。 */
    @Test
    @WithMockUser(roles = "ADMIN")
    void list_usesDefaultsWhenOmitted() throws Exception {
        when(service.search(any(StockSearchRequest.class)))
                .thenReturn(new PageResponse<>(List.of(), new PageResponse.Meta(0, 1, 20, 0)));

        mvc.perform(get(URL)).andExpect(status().isOk());

        ArgumentCaptor<StockSearchRequest> captor = ArgumentCaptor.forClass(StockSearchRequest.class);
        verify(service).search(captor.capture());
        StockSearchRequest r = captor.getValue();
        assertNull(r.getCategoryId());
        assertNull(r.getName());
        assertNull(r.getAmountMin());
        assertEquals("categoryName", r.getSort());
        assertEquals("asc", r.getOrder());
        assertEquals(1, r.getPage());
        assertEquals(20, r.getSize());
    }

    /** 不正なクエリパラメータ（境界の外、許可されない値）は 400（VALIDATION_ERROR）になること。 */
    @Test
    @WithMockUser(roles = "USER")
    void list_rejectsInvalidParameters() throws Exception {
        for (String[] p : new String[][] {
                {"page", "0"}, {"size", "101"}, {"order", "up"}, {"sort", "password"}, {"categoryId", "0"},
                {"centerId", "0"}, {"amountMin", "-1"}, {"amountMax", "-1"}, {"name", "あ".repeat(101)},
                {"categoryId", "abc"}}) {
            mvc.perform(get(URL).param(p[0], p[1]))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }
        verifyNoInteractions(service);
    }

    /** 境界値（個数 0、名称 100 文字）は受け付けること。 */
    @Test
    @WithMockUser(roles = "USER")
    void list_acceptsBoundaryValues() throws Exception {
        when(service.search(any(StockSearchRequest.class)))
                .thenReturn(new PageResponse<>(List.of(), new PageResponse.Meta(0, 1, 20, 0)));

        mvc.perform(get(URL).param("amountMin", "0").param("amountMax", "0").param("name", "あ".repeat(100)))
                .andExpect(status().isOk());
    }

    /** 個数の範囲の矛盾（サービスが BusinessException）は 400（BUSINESS_ERROR）になること。 */
    @Test
    @WithMockUser(roles = "USER")
    void list_mapsBusinessErrorTo400() throws Exception {
        when(service.search(any(StockSearchRequest.class))).thenThrow(new BusinessException("範囲が逆です"));

        mvc.perform(get(URL).param("amountMin", "10").param("amountMax", "5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BUSINESS_ERROR"));
    }

    // ---- 作成 ----

    /** 正しい入力は、サービスに委譲して 200 で結果を返すこと。 */
    @Test
    @WithMockUser(roles = "USER")
    void create_delegatesAndReturnsCreatedStock() throws Exception {
        when(service.create(any(PartsStockRequest.class))).thenReturn(response());

        postBody(validBody())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockId").value(1));

        verify(service).create(new PartsStockRequest(1, "カーボンフレーム", 2, "説明", 85));
    }

    /** 説明は任意で、在庫数 0 と 999,999,999、名称 255 文字の境界は受け付けること。 */
    @Test
    @WithMockUser(roles = "USER")
    void create_acceptsOptionalDescriptionAndBoundaries() throws Exception {
        when(service.create(any(PartsStockRequest.class))).thenReturn(response());
        Map<String, Object> body = validBody();
        body.remove("description");
        body.put("amount", 0);
        postBody(body).andExpect(status().isOk());

        body.put("amount", 999_999_999);
        body.put("name", "あ".repeat(255));
        postBody(body).andExpect(status().isOk());
    }

    /** 必須漏れ、桁数・範囲の超過、{@code <} {@code >} を含む入力は 400 になること。 */
    @Test
    @WithMockUser(roles = "USER")
    void create_rejectsInvalidBody() throws Exception {
        assertCreateRejected("categoryId", null);
        assertCreateRejected("categoryId", 0);
        assertCreateRejected("name", null);
        assertCreateRejected("name", "  ");
        assertCreateRejected("name", "あ".repeat(256));
        assertCreateRejected("name", "<b>");
        assertCreateRejected("centerId", null);
        assertCreateRejected("centerId", 0);
        assertCreateRejected("description", "あ".repeat(256));
        assertCreateRejected("description", "a>b");
        assertCreateRejected("amount", null);
        assertCreateRejected("amount", -1);
        assertCreateRejected("amount", 1_000_000_000);
    }

    /** カテゴリが部品用でない等（BusinessException）は 400、名称の重複は 409 になること。 */
    @Test
    @WithMockUser(roles = "USER")
    void create_mapsBusinessAndConflictErrors() throws Exception {
        when(service.create(any(PartsStockRequest.class)))
                .thenThrow(new BusinessException("カテゴリが違います"))
                .thenThrow(new ConflictException("重複"));

        postBody(validBody()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BUSINESS_ERROR"));
        postBody(validBody()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    // ---- 更新・削除 ----

    /** 更新はパスの ID と本文をサービスに渡し、存在しなければ 404 になること。 */
    @Test
    @WithMockUser(roles = "USER")
    void update_delegatesAndMapsNotFound() throws Exception {
        when(service.update(eq(3), any(PartsStockRequest.class))).thenReturn(response());
        when(service.update(eq(9), any(PartsStockRequest.class))).thenThrow(new NotFoundException("なし"));
        String json = objectMapper.writeValueAsString(validBody());

        mvc.perform(put(URL + "/3").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk());
        mvc.perform(put(URL + "/9").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));

        verify(service).update(3, new PartsStockRequest(1, "カーボンフレーム", 2, "説明", 85));
    }

    /** 削除は {@code {"result":"success"}} を返し、存在しなければ 404 になること。 */
    @Test
    @WithMockUser(roles = "USER")
    void delete_returnsSuccessOrNotFound() throws Exception {
        when(service.delete(1)).thenReturn(Map.of("result", "success"));
        when(service.delete(9)).thenThrow(new NotFoundException("なし"));

        mvc.perform(delete(URL + "/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("success"));
        mvc.perform(delete(URL + "/9")).andExpect(status().isNotFound());
    }
}
