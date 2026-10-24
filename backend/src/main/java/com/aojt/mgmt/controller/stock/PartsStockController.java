package com.aojt.mgmt.controller.stock;

import com.aojt.mgmt.dto.common.PageResponse;
import com.aojt.mgmt.dto.stock.PartsStockRequest;
import com.aojt.mgmt.dto.stock.PartsStockResponse;
import com.aojt.mgmt.dto.stock.StockSearchRequest;
import com.aojt.mgmt.service.stock.PartsStockService;
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
 * 部品在庫 API。USER 権限以上が必要（認可は {@code SecurityConfig} で行う）。
 */
@RestController
@RequestMapping("/api/v1/parts-stocks")
@Tag(name = "Parts stocks", description = "部品在庫 API")
public class PartsStockController {

    private final PartsStockService service;

    /**
     * コントローラーを生成する。
     *
     * @param service 部品在庫のサービス
     */
    public PartsStockController(PartsStockService service) {
        this.service = service;
    }

    /**
     * 部品在庫の一覧を返す（検索、ソート、ページング）。
     *
     * @param request クエリパラメータ
     * @return {@code data} と {@code meta}
     */
    @GetMapping
    public PageResponse<PartsStockResponse> list(@Valid @ParameterObject @ModelAttribute StockSearchRequest request) {
        return service.search(request);
    }

    /**
     * 部品在庫を新規作成する。
     *
     * @param request 作成内容
     * @return 作成した部品在庫
     */
    @PostMapping
    public PartsStockResponse create(@Valid @RequestBody PartsStockRequest request) {
        return service.create(request);
    }

    /**
     * 部品在庫を更新する。
     *
     * @param stockId 在庫ID
     * @param request 更新内容
     * @return 更新後の部品在庫
     */
    @PutMapping("/{stockId}")
    public PartsStockResponse update(@PathVariable Integer stockId, @Valid @RequestBody PartsStockRequest request) {
        return service.update(stockId, request);
    }

    /**
     * 部品在庫を論理削除する。
     *
     * @param stockId 在庫ID
     * @return {@code {"result": "success"}}
     */
    @DeleteMapping("/{stockId}")
    public Map<String, String> delete(@PathVariable Integer stockId) {
        return service.delete(stockId);
    }
}
