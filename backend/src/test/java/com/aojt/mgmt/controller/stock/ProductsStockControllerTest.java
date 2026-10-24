package com.aojt.mgmt.controller.stock;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aojt.mgmt.config.SecurityConfig;
import com.aojt.mgmt.config.SecurityErrorHandlers;
import com.aojt.mgmt.dto.common.PageResponse;
import com.aojt.mgmt.dto.stock.ProductsStockRequest;
import com.aojt.mgmt.dto.stock.ProductsStockResponse;
import com.aojt.mgmt.dto.stock.ProductsStockSearchRequest;
import com.aojt.mgmt.exception.BusinessException;
import com.aojt.mgmt.exception.ConflictException;
import com.aojt.mgmt.exception.NotFoundException;
import com.aojt.mgmt.service.stock.ProductsStockService;
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
 * {@link ProductsStockController} のテスト。部品在庫に加えて、製品コードの入力検証と検索条件を確認する。
 */
@WebMvcTest(ProductsStockController.class)
@Import({SecurityConfig.class, SecurityErrorHandlers.class})
class ProductsStockControllerTest {

    private static final String URL = "/api/v1/products-stocks";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    ProductsStockService service;

    @MockitoBean
    JwtDecoder jwtDecoder;

    private static ProductsStockResponse response() {
        return new ProductsStockResponse(1, "PRD-000001", 11, "災害用ドローン", "災害用ドローン-X", 2, "大阪センター",
                "説明", 12, null, null);
    }

    private static Map<String, Object> validBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("productCode", "PRD-000001");
        body.put("categoryId", 11);
        body.put("name", "災害用ドローン-X");
        body.put("centerId", 2);
        body.put("description", "説明");
        body.put("amount", 12);
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

    /** トークンが無ければ 401、権限のないロールは 403 を返し、サービスを呼ばないこと。 */
    @Test
    void rejectsUnauthenticatedRequests() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_ERROR"));
        postBody(validBody()).andExpect(status().isUnauthorized());
        mvc.perform(delete(URL + "/1")).andExpect(status().isUnauthorized());
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

    /** 製品コードを含むすべての検索条件をサービスに渡し、製品コードを含む結果を返すこと。 */
    @Test
    @WithMockUser(roles = "USER")
    void list_bindsConditionsIncludingProductCode() throws Exception {
        when(service.search(any(ProductsStockSearchRequest.class))).thenReturn(
                new PageResponse<>(List.of(response()), new PageResponse.Meta(1, 1, 20, 1)));

        mvc.perform(get(URL).param("productCode", " PRD ").param("categoryId", "11").param("centerId", "2")
                        .param("name", "ドローン").param("amountMin", "1").param("amountMax", "50")
                        .param("sort", "productCode").param("order", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].productCode").value("PRD-000001"))
                .andExpect(jsonPath("$.data[0].centerName").value("大阪センター"));

        ArgumentCaptor<ProductsStockSearchRequest> captor = ArgumentCaptor.forClass(ProductsStockSearchRequest.class);
        verify(service).search(captor.capture());
        ProductsStockSearchRequest r = captor.getValue();
        assertEquals("PRD", r.getProductCode());
        assertEquals(11, r.getCategoryId());
        assertEquals(2, r.getCenterId());
        assertEquals("ドローン", r.getName());
        assertEquals(1, r.getAmountMin());
        assertEquals(50, r.getAmountMax());
        assertEquals("productCode", r.getSort());
        assertEquals("desc", r.getOrder());
    }

    /** パラメータを省略すると、カテゴリ名の昇順・1 ページ目 20 件で検索すること。 */
    @Test
    @WithMockUser(roles = "ADMIN")
    void list_usesDefaultsWhenOmitted() throws Exception {
        when(service.search(any(ProductsStockSearchRequest.class)))
                .thenReturn(new PageResponse<>(List.of(), new PageResponse.Meta(0, 1, 20, 0)));

        mvc.perform(get(URL)).andExpect(status().isOk());

        ArgumentCaptor<ProductsStockSearchRequest> captor = ArgumentCaptor.forClass(ProductsStockSearchRequest.class);
        verify(service).search(captor.capture());
        assertEquals("categoryName", captor.getValue().getSort());
        assertEquals("asc", captor.getValue().getOrder());
    }

    /** 不正なクエリパラメータは 400（VALIDATION_ERROR）になること。製品コードは 20 文字まで。 */
    @Test
    @WithMockUser(roles = "USER")
    void list_rejectsInvalidParameters() throws Exception {
        for (String[] p : new String[][] {
                {"page", "0"}, {"size", "101"}, {"sort", "deleteFlag"}, {"productCode", "A".repeat(21)},
                {"amountMin", "-1"}}) {
            mvc.perform(get(URL).param(p[0], p[1]))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }
        verifyNoInteractions(service);
    }

    // ---- 作成 ----

    /** 正しい入力は、サービスに委譲して 200 で結果を返すこと。 */
    @Test
    @WithMockUser(roles = "USER")
    void create_delegatesAndReturnsCreatedStock() throws Exception {
        when(service.create(any(ProductsStockRequest.class))).thenReturn(response());

        postBody(validBody())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productCode").value("PRD-000001"));

        verify(service).create(new ProductsStockRequest("PRD-000001", 11, "災害用ドローン-X", 2, "説明", 12));
    }

    /** 製品コードは 20 文字・半角英数字とハイフン、アンダースコアまでを受け付けること。 */
    @Test
    @WithMockUser(roles = "USER")
    void create_acceptsProductCodeBoundaryAndAllowedCharacters() throws Exception {
        when(service.create(any(ProductsStockRequest.class))).thenReturn(response());
        Map<String, Object> body = validBody();
        body.put("productCode", "A".repeat(20));
        postBody(body).andExpect(status().isOk());

        body.put("productCode", "abc_DEF-123");
        postBody(body).andExpect(status().isOk());
    }

    /** 製品コードの必須漏れ、21 文字、使えない文字（日本語、空白、記号、{@code <}）は 400 になること。 */
    @Test
    @WithMockUser(roles = "USER")
    void create_rejectsInvalidProductCode() throws Exception {
        assertCreateRejected("productCode", null);
        assertCreateRejected("productCode", "  ");
        assertCreateRejected("productCode", "A".repeat(21));
        assertCreateRejected("productCode", "製品コード");
        assertCreateRejected("productCode", "PRD 01");
        assertCreateRejected("productCode", "PRD/01");
        assertCreateRejected("productCode", "<PRD>");
    }

    /** その他の項目（カテゴリ、名称、センター、説明、在庫数）の必須・桁数・範囲の違反は 400 になること。 */
    @Test
    @WithMockUser(roles = "USER")
    void create_rejectsInvalidCommonFields() throws Exception {
        assertCreateRejected("categoryId", null);
        assertCreateRejected("name", "");
        assertCreateRejected("name", "あ".repeat(256));
        assertCreateRejected("name", "<i>");
        assertCreateRejected("centerId", null);
        assertCreateRejected("description", "あ".repeat(256));
        assertCreateRejected("amount", -1);
        assertCreateRejected("amount", 1_000_000_000);
        assertCreateRejected("amount", null);
    }

    /** カテゴリが製品用でない等は 400、製品コードの重複は 409 になること。 */
    @Test
    @WithMockUser(roles = "USER")
    void create_mapsBusinessAndConflictErrors() throws Exception {
        when(service.create(any(ProductsStockRequest.class)))
                .thenThrow(new BusinessException("カテゴリが違います"))
                .thenThrow(new ConflictException("同じ製品コードが既に登録されています。"));

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
        when(service.update(eq(3), any(ProductsStockRequest.class))).thenReturn(response());
        when(service.update(eq(9), any(ProductsStockRequest.class))).thenThrow(new NotFoundException("なし"));
        String json = objectMapper.writeValueAsString(validBody());

        mvc.perform(put(URL + "/3").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk());
        mvc.perform(put(URL + "/9").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isNotFound());

        verify(service).update(3, new ProductsStockRequest("PRD-000001", 11, "災害用ドローン-X", 2, "説明", 12));
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
