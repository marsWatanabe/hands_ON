package com.aojt.mgmt.controller.category;

import static org.hamcrest.Matchers.containsString;
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
import com.aojt.mgmt.dto.category.CategoryRequest;
import com.aojt.mgmt.dto.category.CategoryResponse;
import com.aojt.mgmt.dto.category.CategorySearchRequest;
import com.aojt.mgmt.dto.common.PageResponse;
import com.aojt.mgmt.exception.BusinessException;
import com.aojt.mgmt.exception.ConflictException;
import com.aojt.mgmt.exception.NotFoundException;
import com.aojt.mgmt.service.category.CategoryService;
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
 * {@link CategoryController} のテスト。サービスをモックにし、認証・認可、入力検証、エラー応答の形式、
 * サービスへの委譲を確認する。
 */
@WebMvcTest(CategoryController.class)
@Import({SecurityConfig.class, SecurityErrorHandlers.class})
class CategoryControllerTest {

    private static final String URL = "/api/v1/categories";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    CategoryService service;

    @MockitoBean
    JwtDecoder jwtDecoder;

    private static CategoryResponse response() {
        return new CategoryResponse(1, "part", "フレーム", "説明", null, null);
    }

    private static Map<String, Object> validBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("categoryType", "part");
        body.put("categoryName", "フレーム");
        body.put("description", "説明");
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
        mvc.perform(get(URL))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verifyNoInteractions(service);
    }

    /** 書き込み系のエンドポイントも、トークンが無ければ 401 になること。 */
    @Test
    void writeEndpointsReturn401WithoutAuthentication() throws Exception {
        postBody(validBody()).andExpect(status().isUnauthorized());
        mvc.perform(put(URL + "/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete(URL + "/1")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    // ---- 一覧 ----

    /** クエリパラメータをサービスに渡し、{@code data} と {@code meta} を返すこと。 */
    @Test
    @WithMockUser(roles = "USER")
    void list_bindsParametersAndReturnsPage() throws Exception {
        when(service.search(any(CategorySearchRequest.class))).thenReturn(
                new PageResponse<>(List.of(response()), new PageResponse.Meta(1, 2, 5, 3)));

        mvc.perform(get(URL).param("categoryType", "part").param("categoryName", " フレ ")
                        .param("page", "2").param("size", "5").param("sort", "categoryName").param("order", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].categoryName").value("フレーム"))
                .andExpect(jsonPath("$.meta.total").value(1))
                .andExpect(jsonPath("$.meta.page").value(2));

        ArgumentCaptor<CategorySearchRequest> captor = ArgumentCaptor.forClass(CategorySearchRequest.class);
        verify(service).search(captor.capture());
        CategorySearchRequest r = captor.getValue();
        assertEquals("part", r.getCategoryType());
        assertEquals("フレ", r.getCategoryName());
        assertEquals(2, r.getPage());
        assertEquals(5, r.getSize());
        assertEquals("categoryName", r.getSort());
        assertEquals("asc", r.getOrder());
    }

    /** パラメータを省略すると、既定値（1 ページ目、20 件、種別の降順）で検索すること。 */
    @Test
    @WithMockUser(roles = "ADMIN")
    void list_usesDefaultsWhenOmitted() throws Exception {
        when(service.search(any(CategorySearchRequest.class)))
                .thenReturn(new PageResponse<>(List.of(), new PageResponse.Meta(0, 1, 20, 0)));

        mvc.perform(get(URL)).andExpect(status().isOk());

        ArgumentCaptor<CategorySearchRequest> captor = ArgumentCaptor.forClass(CategorySearchRequest.class);
        verify(service).search(captor.capture());
        assertEquals(1, captor.getValue().getPage());
        assertEquals(20, captor.getValue().getSize());
        assertEquals("categoryType", captor.getValue().getSort());
        assertEquals("desc", captor.getValue().getOrder());
    }

    /** 不正なクエリパラメータ（境界の外、許可されない値）は 400（VALIDATION_ERROR）になること。 */
    @Test
    @WithMockUser(roles = "USER")
    void list_rejectsInvalidParameters() throws Exception {
        for (String[] p : new String[][] {
                {"page", "0"}, {"size", "0"}, {"size", "101"}, {"order", "up"}, {"sort", "deleteFlag"},
                {"categoryType", "other"}, {"categoryName", "あ".repeat(21)}}) {
            mvc.perform(get(URL).param(p[0], p[1]))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }
        verifyNoInteractions(service);
    }

    /** 境界値（size=100、カテゴリ名 20 文字）は受け付けること。 */
    @Test
    @WithMockUser(roles = "USER")
    void list_acceptsBoundaryValues() throws Exception {
        when(service.search(any(CategorySearchRequest.class)))
                .thenReturn(new PageResponse<>(List.of(), new PageResponse.Meta(0, 1, 100, 0)));

        mvc.perform(get(URL).param("size", "100").param("categoryName", "あ".repeat(20)))
                .andExpect(status().isOk());
    }

    // ---- 作成 ----

    /** 正しい入力は、サービスに委譲して 200 で結果を返すこと。 */
    @Test
    @WithMockUser(roles = "USER")
    void create_delegatesAndReturnsCreatedCategory() throws Exception {
        when(service.create(any(CategoryRequest.class))).thenReturn(response());

        postBody(validBody())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.deleteFlag").doesNotExist());

        verify(service).create(new CategoryRequest("part", "フレーム", "説明"));
    }

    /** 説明は任意で、カテゴリ名 20 文字・説明 255 文字（境界）は受け付けること。 */
    @Test
    @WithMockUser(roles = "USER")
    void create_acceptsOptionalDescriptionAndBoundaryLengths() throws Exception {
        when(service.create(any(CategoryRequest.class))).thenReturn(response());
        Map<String, Object> body = validBody();
        body.remove("description");
        postBody(body).andExpect(status().isOk());

        body.put("categoryName", "あ".repeat(20));
        body.put("description", "あ".repeat(255));
        postBody(body).andExpect(status().isOk());
    }

    /** 必須漏れ、形式違反、桁数超過、{@code <} {@code >} を含む入力は 400 になること。 */
    @Test
    @WithMockUser(roles = "USER")
    void create_rejectsInvalidBody() throws Exception {
        assertCreateRejected("categoryType", null);
        assertCreateRejected("categoryType", "other");
        assertCreateRejected("categoryName", null);
        assertCreateRejected("categoryName", "   ");
        assertCreateRejected("categoryName", "あ".repeat(21));
        assertCreateRejected("categoryName", "<b>タグ</b>");
        assertCreateRejected("description", "あ".repeat(256));
        assertCreateRejected("description", "<script>");
    }

    /** メッセージは日本語で、どの項目の誤りかが分かること。 */
    @Test
    @WithMockUser(roles = "USER")
    void create_returnsJapaneseValidationMessage() throws Exception {
        Map<String, Object> body = validBody();
        body.put("categoryName", "あ".repeat(21));

        postBody(body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message", containsString("カテゴリ名は20文字以内")));
    }

    /** JSON として読めない本文は 400 になること。 */
    @Test
    @WithMockUser(roles = "USER")
    void create_rejectsMalformedJson() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    /** 同じ種別のカテゴリ名の重複は 409（CONFLICT）になること。 */
    @Test
    @WithMockUser(roles = "USER")
    void create_mapsDuplicateTo409() throws Exception {
        when(service.create(any(CategoryRequest.class))).thenThrow(new ConflictException("重複"));

        postBody(validBody())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                .andExpect(jsonPath("$.error.message").value("重複"));
    }

    // ---- 更新 ----

    /** 更新はパスの ID と本文をサービスに渡すこと。 */
    @Test
    @WithMockUser(roles = "USER")
    void update_delegatesWithPathId() throws Exception {
        when(service.update(eq(3), any(CategoryRequest.class))).thenReturn(response());

        mvc.perform(put(URL + "/3").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryName").value("フレーム"));

        verify(service).update(3, new CategoryRequest("part", "フレーム", "説明"));
    }

    /** 存在しないカテゴリの更新は 404（NOT_FOUND）、種別変更の禁止は 400（BUSINESS_ERROR）になること。 */
    @Test
    @WithMockUser(roles = "USER")
    void update_mapsNotFoundAndBusinessErrors() throws Exception {
        when(service.update(eq(9), any(CategoryRequest.class))).thenThrow(new NotFoundException("なし"));
        when(service.update(eq(1), any(CategoryRequest.class))).thenThrow(new BusinessException("使用中"));
        String json = objectMapper.writeValueAsString(validBody());

        mvc.perform(put(URL + "/9").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        mvc.perform(put(URL + "/1").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BUSINESS_ERROR"));
    }

    /** ID が数値でない場合は 400 になること。 */
    @Test
    @WithMockUser(roles = "USER")
    void update_rejectsNonNumericId() throws Exception {
        mvc.perform(put(URL + "/abc").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    // ---- 削除 ----

    /** 削除はサービスに委譲し、{@code {"result":"success"}} を返すこと。 */
    @Test
    @WithMockUser(roles = "USER")
    void delete_returnsSuccess() throws Exception {
        when(service.delete(1)).thenReturn(Map.of("result", "success"));

        mvc.perform(delete(URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("success"));
    }

    /** 在庫で使用中のカテゴリの削除は 400（BUSINESS_ERROR）、存在しなければ 404 になること。 */
    @Test
    @WithMockUser(roles = "USER")
    void delete_mapsBusinessAndNotFoundErrors() throws Exception {
        when(service.delete(1)).thenThrow(new BusinessException("使用中"));
        when(service.delete(9)).thenThrow(new NotFoundException("なし"));

        mvc.perform(delete(URL + "/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BUSINESS_ERROR"));
        mvc.perform(delete(URL + "/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }
}
