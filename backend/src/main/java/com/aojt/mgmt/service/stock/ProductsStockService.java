package com.aojt.mgmt.service.stock;

import com.aojt.mgmt.dto.common.PageResponse;
import com.aojt.mgmt.dto.stock.ProductsStockRequest;
import com.aojt.mgmt.dto.stock.ProductsStockResponse;
import com.aojt.mgmt.dto.stock.ProductsStockSearchRequest;
import com.aojt.mgmt.exception.BusinessException;
import com.aojt.mgmt.exception.ConflictException;
import com.aojt.mgmt.exception.NotFoundException;
import com.aojt.mgmt.model.Category;
import com.aojt.mgmt.model.Center;
import com.aojt.mgmt.model.ProductsStock;
import com.aojt.mgmt.model.StockItem;
import com.aojt.mgmt.repository.stock.ProductsStockRepository;
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
 * 製品在庫の業務ロジック。検索、作成、更新、論理削除を行う。
 */
@Service
public class ProductsStockService {

    /** ソート項目の許可リスト（部品在庫の項目に、製品コードを加えたもの）。 */
    static final Set<String> SORTABLE_FIELDS = Set.of(
            "stockId", "name", "amount", "categoryName", "centerName", "createDate", "updateDate", "productCode");

    /** 削除成功時のレスポンスのキーと値。 */
    private static final Map<String, String> DELETE_SUCCESS = Map.of("result", "success");

    private static final String MSG_NOT_FOUND = "製品在庫が見つかりません。ID: ";
    private static final String MSG_NAME_EXISTS = "同じ在庫センターに同じ名称の製品在庫が既に登録されています。";
    private static final String MSG_CODE_EXISTS = "同じ製品コードが既に登録されています。";
    private static final String MSG_AMOUNT_RANGE = "個数（以上）は個数（以下）以下にしてください。";

    private final ProductsStockRepository repository;
    private final StockReferenceResolver references;

    /**
     * サービスを生成する。
     *
     * @param repository 製品在庫のリポジトリ
     * @param references カテゴリとセンターの検証
     */
    public ProductsStockService(ProductsStockRepository repository, StockReferenceResolver references) {
        this.repository = repository;
        this.references = references;
    }

    /**
     * 製品在庫を検索する。論理削除済みは含まない。カテゴリ名とセンター名は結合して返す。
     *
     * @param request 検索条件、ソート、ページング
     * @return 検索結果（{@code data} と {@code meta}）
     * @throws BusinessException ソート項目が許可リストに無い場合、または個数の範囲が逆の場合
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductsStockResponse> search(ProductsStockSearchRequest request) {
        if (request.getAmountMin() != null && request.getAmountMax() != null
                && request.getAmountMin() > request.getAmountMax()) {
            throw new BusinessException(MSG_AMOUNT_RANGE);
        }
        Specification<ProductsStock> specification = StockSpecifications.withProductCode(
                StockSpecifications.search(request.getCategoryId(), request.getCenterId(), request.getName(),
                        request.getAmountMin(), request.getAmountMax()),
                request.getProductCode());
        Sort sort = StockSorts.resolve(request.getSort(), request, SORTABLE_FIELDS);
        if (PagingSupport.exceedsJpaOffset(request)) {
            return PageResponse.empty(repository.count(specification), request.getPage(), request.getSize());
        }
        Page<ProductsStockResponse> page = repository
                .findAll(specification, PageRequest.of(request.getPage() - 1, request.getSize(), sort))
                .map(ProductsStockResponse::from);
        return PageResponse.of(page);
    }

    /**
     * 製品在庫を新規作成する。
     *
     * @param request 作成内容
     * @return 作成した製品在庫
     * @throws BusinessException カテゴリが存在しない・製品用でない、またはセンターが存在しない場合
     * @throws ConflictException 製品コードが使われている、または同じセンターに同じ名称の未削除の製品在庫がある場合
     */
    @Transactional
    public ProductsStockResponse create(ProductsStockRequest request) {
        Category category = references.requireCategory(request.categoryId(), Category.TYPE_PRODUCT);
        Center center = references.requireCenter(request.centerId());
        if (repository.existsByProductCode(request.productCode())) {
            throw new ConflictException(MSG_CODE_EXISTS);
        }
        if (repository.existsByCenter_CenterIdAndNameAndDeleteFlag(
                request.centerId(), request.name(), StockItem.NOT_DELETED)) {
            throw new ConflictException(MSG_NAME_EXISTS);
        }
        ProductsStock stock = new ProductsStock();
        apply(stock, category, center, request);
        // 重複確認から保存までの間の競合は DB のユニーク制約が検出する（409 になる）。
        return ProductsStockResponse.from(repository.saveAndFlush(stock));
    }

    /**
     * 製品在庫を更新する（全項目を置き換える）。
     *
     * @param stockId 在庫ID
     * @param request 更新内容
     * @return 更新後の製品在庫
     * @throws NotFoundException 製品在庫が存在しない、または削除済みの場合
     * @throws BusinessException カテゴリが存在しない・製品用でない、またはセンターが存在しない場合
     * @throws ConflictException 他の製品在庫が製品コードを使っている、または同じセンターに同じ名称の他の未削除の
     *         製品在庫がある場合
     */
    @Transactional
    public ProductsStockResponse update(Integer stockId, ProductsStockRequest request) {
        ProductsStock stock = findActive(stockId);
        Category category = references.requireCategory(request.categoryId(), Category.TYPE_PRODUCT);
        Center center = references.requireCenter(request.centerId());
        if (repository.existsByProductCodeAndStockIdNot(request.productCode(), stockId)) {
            throw new ConflictException(MSG_CODE_EXISTS);
        }
        if (repository.existsByCenter_CenterIdAndNameAndDeleteFlagAndStockIdNot(
                request.centerId(), request.name(), StockItem.NOT_DELETED, stockId)) {
            throw new ConflictException(MSG_NAME_EXISTS);
        }
        apply(stock, category, center, request);
        return ProductsStockResponse.from(repository.saveAndFlush(stock));
    }

    /**
     * 製品在庫を論理削除する（{@code delete_flag = 1}）。製品コードは削除後も再利用できない。
     *
     * @param stockId 在庫ID
     * @return {@code {"result": "success"}}
     * @throws NotFoundException 製品在庫が存在しない、または削除済みの場合
     */
    @Transactional
    public Map<String, String> delete(Integer stockId) {
        findActive(stockId).markDeleted();
        return DELETE_SUCCESS;
    }

    private ProductsStock findActive(Integer stockId) {
        return repository.findByStockIdAndDeleteFlag(stockId, StockItem.NOT_DELETED)
                .orElseThrow(() -> new NotFoundException(MSG_NOT_FOUND + stockId));
    }

    /** 検証済みの参照先（カテゴリ、センター）と、リクエストの内容をエンティティに反映する。 */
    private static void apply(ProductsStock stock, Category category, Center center, ProductsStockRequest r) {
        stock.setCategory(category);
        stock.setCenter(center);
        stock.setProductCode(r.productCode());
        stock.setName(r.name());
        stock.setDescription(r.description());
        stock.setAmount(r.amount());
    }
}
