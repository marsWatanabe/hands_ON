package com.aojt.mgmt.service.category;

import com.aojt.mgmt.dto.category.CategoryRequest;
import com.aojt.mgmt.dto.category.CategoryResponse;
import com.aojt.mgmt.dto.category.CategorySearchRequest;
import com.aojt.mgmt.dto.common.PageResponse;
import com.aojt.mgmt.exception.BusinessException;
import com.aojt.mgmt.exception.ConflictException;
import com.aojt.mgmt.exception.NotFoundException;
import com.aojt.mgmt.model.Category;
import com.aojt.mgmt.repository.category.CategoryRepository;
import com.aojt.mgmt.repository.category.CategorySpecifications;
import com.aojt.mgmt.repository.stock.PartsStockRepository;
import com.aojt.mgmt.repository.stock.ProductsStockRepository;
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
 * カテゴリの業務ロジック。検索、作成、更新、論理削除を行う。
 */
@Service
public class CategoryService {

    /** ソート項目の許可リスト。エンティティの項目名と一致させる。 */
    static final Set<String> SORTABLE_FIELDS = Set.of(
            "categoryId", "categoryType", "categoryName", "createDate", "updateDate");

    /** 同じ値が並んでもページ間で順序が変わらないようにする、最後のソートキー。 */
    private static final String TIE_BREAKER = "categoryId";

    /** 種別でソートするときの、第 2 ソートキー（画面仕様の「種別、カテゴリ名」の並び）。 */
    private static final String TYPE_SECONDARY = "categoryName";

    /** 削除成功時のレスポンスのキーと値。 */
    private static final Map<String, String> DELETE_SUCCESS = Map.of("result", "success");

    private static final String MSG_NAME_EXISTS = "同じ種別に同じカテゴリ名が既に登録されています。";
    private static final String MSG_NOT_FOUND = "カテゴリが見つかりません。ID: ";
    private static final String MSG_INVALID_SORT = "並び替え項目が不正です。";
    private static final String MSG_IN_USE_DELETE = "カテゴリは在庫で使用されているため削除できません。";
    private static final String MSG_IN_USE_TYPE = "カテゴリは在庫で使用されているため、種別を変更できません。";

    private final CategoryRepository repository;
    private final PartsStockRepository partsStockRepository;
    private final ProductsStockRepository productsStockRepository;

    /**
     * サービスを生成する。
     *
     * @param repository カテゴリのリポジトリ
     * @param partsStockRepository 部品在庫のリポジトリ（使用中の確認に使う）
     * @param productsStockRepository 製品在庫のリポジトリ（使用中の確認に使う）
     */
    public CategoryService(CategoryRepository repository, PartsStockRepository partsStockRepository,
            ProductsStockRepository productsStockRepository) {
        this.repository = repository;
        this.partsStockRepository = partsStockRepository;
        this.productsStockRepository = productsStockRepository;
    }

    /**
     * カテゴリを検索する。論理削除済みは含まない。
     *
     * @param request 検索条件、ソート、ページング
     * @return 検索結果（{@code data} と {@code meta}）
     * @throws BusinessException ソート項目が許可リストに無い場合
     */
    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> search(CategorySearchRequest request) {
        Specification<Category> specification = CategorySpecifications.search(
                request.getCategoryType(), request.getCategoryName());
        Sort sort = toSort(request);
        if (PagingSupport.exceedsJpaOffset(request)) {
            return PageResponse.empty(repository.count(specification), request.getPage(), request.getSize());
        }
        Page<CategoryResponse> page = repository
                .findAll(specification, PageRequest.of(request.getPage() - 1, request.getSize(), sort))
                .map(CategoryResponse::from);
        return PageResponse.of(page);
    }

    /**
     * カテゴリを新規作成する。
     *
     * @param request 作成内容
     * @return 作成したカテゴリ
     * @throws ConflictException 同じ種別に同じカテゴリ名の未削除のカテゴリがある場合
     */
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (repository.existsByCategoryTypeAndCategoryNameAndDeleteFlag(
                request.categoryType(), request.categoryName(), Category.NOT_DELETED)) {
            throw new ConflictException(MSG_NAME_EXISTS);
        }
        Category category = new Category();
        apply(category, request);
        return CategoryResponse.from(repository.saveAndFlush(category));
    }

    /**
     * カテゴリを更新する（全項目を置き換える）。
     *
     * @param categoryId カテゴリID
     * @param request 更新内容
     * @return 更新後のカテゴリ
     * @throws NotFoundException カテゴリが存在しない、または削除済みの場合
     * @throws ConflictException 同じ種別に同じカテゴリ名の他のカテゴリがある場合
     * @throws BusinessException 在庫で使用中のカテゴリの種別を変更しようとした場合
     */
    @Transactional
    public CategoryResponse update(Integer categoryId, CategoryRequest request) {
        Category category = findActive(categoryId);
        if (!category.getCategoryType().equals(request.categoryType()) && isInUse(categoryId)) {
            throw new BusinessException(MSG_IN_USE_TYPE);
        }
        if (repository.existsByCategoryTypeAndCategoryNameAndDeleteFlagAndCategoryIdNot(
                request.categoryType(), request.categoryName(), Category.NOT_DELETED, categoryId)) {
            throw new ConflictException(MSG_NAME_EXISTS);
        }
        apply(category, request);
        return CategoryResponse.from(repository.saveAndFlush(category));
    }

    /**
     * カテゴリを論理削除する（{@code delete_flag = 1}）。
     *
     * @param categoryId カテゴリID
     * @return {@code {"result": "success"}}
     * @throws NotFoundException カテゴリが存在しない、または削除済みの場合
     * @throws BusinessException 未削除の在庫から参照されている場合
     */
    @Transactional
    public Map<String, String> delete(Integer categoryId) {
        Category category = findActive(categoryId);
        if (isInUse(categoryId)) {
            throw new BusinessException(MSG_IN_USE_DELETE);
        }
        category.markDeleted();
        return DELETE_SUCCESS;
    }

    private Category findActive(Integer categoryId) {
        return repository.findByCategoryIdAndDeleteFlag(categoryId, Category.NOT_DELETED)
                .orElseThrow(() -> new NotFoundException(MSG_NOT_FOUND + categoryId));
    }

    /** 未削除の部品在庫または製品在庫から参照されているかを返す。 */
    private boolean isInUse(Integer categoryId) {
        return partsStockRepository.existsByCategory_CategoryIdAndDeleteFlag(categoryId, Category.NOT_DELETED)
                || productsStockRepository.existsByCategory_CategoryIdAndDeleteFlag(categoryId, Category.NOT_DELETED);
    }

    private static void apply(Category category, CategoryRequest r) {
        category.setCategoryType(r.categoryType());
        category.setCategoryName(r.categoryName());
        category.setDescription(r.description());
    }

    private static Sort toSort(CategorySearchRequest request) {
        if (!SORTABLE_FIELDS.contains(request.getSort())) {
            throw new BusinessException(MSG_INVALID_SORT);
        }
        Sort.Direction direction = PagingSupport.direction(request);
        Sort sort = Sort.by(direction, request.getSort());
        if ("categoryType".equals(request.getSort())) {
            sort = sort.and(Sort.by(direction, TYPE_SECONDARY));
        }
        if (!TIE_BREAKER.equals(request.getSort())) {
            sort = sort.and(Sort.by(Sort.Direction.ASC, TIE_BREAKER));
        }
        return sort;
    }
}
