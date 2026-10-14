package com.aojt.mgmt.controller.center;

import com.aojt.mgmt.dto.center.CenterRequest;
import com.aojt.mgmt.dto.center.CenterResponse;
import com.aojt.mgmt.dto.center.CenterSearchRequest;
import com.aojt.mgmt.dto.common.PageResponse;
import com.aojt.mgmt.service.center.CenterService;
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
 * 在庫センター API。USER 権限以上が必要（認可は {@code SecurityConfig} で行う）。
 */
@RestController
@RequestMapping("/api/v1/centers")
@Tag(name = "Centers", description = "在庫センター API")
public class CenterController {

    private final CenterService service;

    /**
     * コントローラーを生成する。
     *
     * @param service 在庫センターのサービス
     */
    public CenterController(CenterService service) {
        this.service = service;
    }

    /**
     * 在庫センターの一覧を返す（検索、ソート、ページング）。
     *
     * @param request クエリパラメータ
     * @return {@code data} と {@code meta}
     */
    @GetMapping
    public PageResponse<CenterResponse> list(@Valid @ParameterObject @ModelAttribute CenterSearchRequest request) {
        return service.search(request);
    }

    /**
     * 在庫センターを新規作成する。
     *
     * @param request 作成内容
     * @return 作成した在庫センター
     */
    @PostMapping
    public CenterResponse create(@Valid @RequestBody CenterRequest request) {
        return service.create(request);
    }

    /**
     * 在庫センターを更新する。
     *
     * @param centerId 在庫センターID
     * @param request 更新内容
     * @return 更新後の在庫センター
     */
    @PutMapping("/{centerId}")
    public CenterResponse update(@PathVariable Integer centerId, @Valid @RequestBody CenterRequest request) {
        return service.update(centerId, request);
    }

    /**
     * 在庫センターを論理削除する。
     *
     * @param centerId 在庫センターID
     * @return {@code {"result": "success"}}
     */
    @DeleteMapping("/{centerId}")
    public Map<String, String> delete(@PathVariable Integer centerId) {
        return service.delete(centerId);
    }
}
