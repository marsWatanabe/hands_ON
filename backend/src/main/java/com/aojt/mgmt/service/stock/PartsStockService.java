package com.aojt.mgmt.service.stock;

import com.aojt.mgmt.dto.common.PageResponse;
import com.aojt.mgmt.dto.stock.PartsStockRequest;
import com.aojt.mgmt.dto.stock.PartsStockResponse;
import com.aojt.mgmt.dto.stock.StockSearchRequest;
import com.aojt.mgmt.exception.BusinessException;
import com.aojt.mgmt.exception.ConflictException;
import com.aojt.mgmt.exception.NotFoundException;
import com.aojt.mgmt.model.Category;
import com.aojt.mgmt.model.Center;
import com.aojt.mgmt.model.PartsStock;
import com.aojt.mgmt.model.StockItem;
import com.aojt.mgmt.repository.stock.PartsStockRepository;
import com.aojt.mgmt.repository.stock.StockSpecifications;
import com.aojt.mgmt.service.common.PagingSupport;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 部品在庫の業務ロジック。検索、作成、更新、論理削除を行う。
 */
@Service
public class PartsStockService {

    /** ソート項目の許可リスト。 */
    static final Set<String> SORTABLE_FIELDS = Set.of(
            "stockId", "name", "amount", "categoryName", "centerName", "createDate", "updateDate");

    /** 削除成功時のレスポンスのキーと値。 */
    private static final Map<String, String> DELETE_SUCCESS = Map.of("result", "success");

    private static final String MSG_NOT_FOUND = "部品在庫が見つかりません。ID: ";
    private static final String MSG_NAME_EXISTS = "同じ在庫センターに同じ名称の部品在庫が既に登録されています。";
    private static final String MSG_AMOUNT_RANGE = "個数（以上）は個数（以下）以下にしてください。";

    private final PartsStockRepository repository;
    private final StockReferenceResolver references;

    /**
     * サービスを生成する。
     *
     * @param repository 部品在庫のリポジトリ
     * @param references カテゴリとセンターの検証
     */
    public PartsStockService(PartsStockRepository repository, StockReferenceResolver references) {
        this.repository = repository;
        this.references = references;
    }

    /**
     * 部品在庫を検索する。論理削除済みは含まない。カテゴリ名とセンター名は結合して返す。
     *
     * @param request 検索条件、ソート、ページング
     * @return 検索結果（{@code data} と {@code meta}）
     * @throws BusinessException ソート項目が許可リストに無い場合、または個数の範囲が逆の場合
     */
    @Transactional(readOnly = true)
    public PageResponse<PartsStockResponse> search(StockSearchRequest request) {
        validateAmountRange(request.getAmountMin(), request.getAmountMax());
        Specification<PartsStock> specification = StockSpecifications.search(
                request.getCategoryId(), request.getCenterId(), request.getName(),
                request.getAmountMin(), request.getAmountMax());
        Sort sort = StockSorts.resolve(request.getSort(), request, SORTABLE_FIELDS);
        if (PagingSupport.exceedsJpaOffset(request)) {
            return PageResponse.empty(repository.count(specification), request.getPage(), request.getSize());
        }
        Page<PartsStockResponse> page = repository
                .findAll(specification, PageRequest.of(request.getPage() - 1, request.getSize(), sort))
                .map(PartsStockResponse::from);
        return PageResponse.of(page);
    }

    /**
     * 部品在庫を新規作成する。
     *
     * @param request 作成内容
     * @return 作成した部品在庫
     * @throws BusinessException カテゴリが存在しない・部品用でない、またはセンターが存在しない場合
     * @throws ConflictException 同じセンターに同じ名称の未削除の部品在庫がある場合
     */
    @Transactional
    public PartsStockResponse create(PartsStockRequest request) {
        Category category = references.requireCategory(request.categoryId(), Category.TYPE_PART);
        Center center = references.requireCenter(request.centerId());
        if (repository.existsByCenter_CenterIdAndNameAndDeleteFlag(
                request.centerId(), request.name(), StockItem.NOT_DELETED)) {
            throw new ConflictException(MSG_NAME_EXISTS);
        }
        PartsStock stock = new PartsStock();
        apply(stock, category, center, request);
        return PartsStockResponse.from(repository.saveAndFlush(stock));
    }

    /**
     * 部品在庫を更新する（全項目を置き換える）。
     *
     * @param stockId 在庫ID
     * @param request 更新内容
     * @return 更新後の部品在庫
     * @throws NotFoundException 部品在庫が存在しない、または削除済みの場合
     * @throws BusinessException カテゴリが存在しない・部品用でない、またはセンターが存在しない場合
     * @throws ConflictException 同じセンターに同じ名称の他の未削除の部品在庫がある場合
     */
    @Transactional
    public PartsStockResponse update(Integer stockId, PartsStockRequest request) {
        PartsStock stock = findActive(stockId);
        Category category = references.requireCategory(request.categoryId(), Category.TYPE_PART);
        Center center = references.requireCenter(request.centerId());
        if (repository.existsByCenter_CenterIdAndNameAndDeleteFlagAndStockIdNot(
                request.centerId(), request.name(), StockItem.NOT_DELETED, stockId)) {
            throw new ConflictException(MSG_NAME_EXISTS);
        }
        apply(stock, category, center, request);
        return PartsStockResponse.from(repository.saveAndFlush(stock));
    }

    /**
     * 部品在庫を論理削除する（{@code delete_flag = 1}）。
     *
     * @param stockId 在庫ID
     * @return {@code {"result": "success"}}
     * @throws NotFoundException 部品在庫が存在しない、または削除済みの場合
     */
    @Transactional
    public Map<String, String> delete(Integer stockId) {
        findActive(stockId).markDeleted();
        return DELETE_SUCCESS;
    }

    private PartsStock findActive(Integer stockId) {
        return repository.findByStockIdAndDeleteFlag(stockId, StockItem.NOT_DELETED)
                .orElseThrow(() -> new NotFoundException(MSG_NOT_FOUND + stockId));
    }

    /** 検証済みの参照先（カテゴリ、センター）と、リクエストの内容をエンティティに反映する。 */
    private static void apply(PartsStock stock, Category category, Center center, PartsStockRequest r) {
        stock.setCategory(category);
        stock.setCenter(center);
        stock.setName(r.name());
        stock.setDescription(r.description());
        stock.setAmount(r.amount());
    }

    private static void validateAmountRange(Integer min, Integer max) {
        if (min != null && max != null && min > max) {
            throw new BusinessException(MSG_AMOUNT_RANGE);
        }
    }
}
