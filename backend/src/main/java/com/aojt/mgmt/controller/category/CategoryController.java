package com.aojt.mgmt.controller.category;

import com.aojt.mgmt.dto.category.CategoryRequest;
import com.aojt.mgmt.dto.category.CategoryResponse;
import com.aojt.mgmt.dto.category.CategorySearchRequest;
import com.aojt.mgmt.dto.common.PageResponse;
import com.aojt.mgmt.service.category.CategoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * カテゴリ API。USER 権限以上が必要（認可は {@code SecurityConfig} で行う）。
 */
@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories", description = "カテゴリ API")
public class CategoryController {

    private final CategoryService service;

    /**
     * コントローラーを生成する。
     *
     * @param service カテゴリのサービス
     */
    public CategoryController(CategoryService service) {
        this.service = service;
    }

    /**
     * カテゴリの一覧を返す（検索、ソート、ページング）。
     *
     * @param request クエリパラメータ
     * @return {@code data} と {@code meta}
     */
    @GetMapping
    public PageResponse<CategoryResponse> list(@Valid @ParameterObject @ModelAttribute CategorySearchRequest request) {
        return service.search(request);
    }

    /**
     * カテゴリを新規作成する。
     *
     * @param request 作成内容
     * @return 作成したカテゴリ
     */
    @PostMapping
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request) {
        return service.create(request);
    }

    /**
     * カテゴリを更新する。
     *
     * @param categoryId カテゴリID
     * @param request 更新内容
     * @return 更新後のカテゴリ
     */
    @PutMapping("/{categoryId}")
    public CategoryResponse update(@PathVariable Integer categoryId, @Valid @RequestBody CategoryRequest request) {
        return service.update(categoryId, request);
    }

    /**
     * カテゴリを論理削除する。在庫で使用中のカテゴリは削除できない。
     *
     * @param categoryId カテゴリID
     * @return {@code {"result": "success"}}
     */
    @DeleteMapping("/{categoryId}")
    public Map<String, String> delete(@PathVariable Integer categoryId) {
        return service.delete(categoryId);
    }
}
