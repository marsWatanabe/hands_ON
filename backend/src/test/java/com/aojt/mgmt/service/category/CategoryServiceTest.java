package com.aojt.mgmt.service.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.aojt.mgmt.dto.category.CategoryRequest;
import com.aojt.mgmt.dto.category.CategoryResponse;
import com.aojt.mgmt.dto.category.CategorySearchRequest;
import com.aojt.mgmt.dto.common.PageResponse;
import com.aojt.mgmt.exception.BusinessException;
import com.aojt.mgmt.exception.ConflictException;
import com.aojt.mgmt.exception.NotFoundException;
import com.aojt.mgmt.model.Category;
import com.aojt.mgmt.repository.category.CategoryRepository;
import com.aojt.mgmt.repository.stock.PartsStockRepository;
import com.aojt.mgmt.repository.stock.ProductsStockRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

/**
 * {@link CategoryService} の単体テスト。リポジトリをモックにし、検索のソート、種別内の名称重複、
 * 在庫で使用中のカテゴリの削除・種別変更の禁止、論理削除の挙動を確認する。
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    CategoryRepository repository;

    @Mock
    PartsStockRepository partsStockRepository;

    @Mock
    ProductsStockRepository productsStockRepository;

    @InjectMocks
    CategoryService service;

    private static CategoryRequest request(String type, String name) {
        return new CategoryRequest(type, name, "説明");
    }

    private static Category category(String type, String name) {
        Category c = new Category();
        c.setCategoryType(type);
        c.setCategoryName(name);
        c.setDescription("説明");
        return c;
    }

    @SuppressWarnings("unchecked")
    private Pageable searchAndCapturePageable(CategorySearchRequest request) {
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        service.search(request);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), captor.capture());
        return captor.getValue();
    }

    // ---- search ----

    /** 検索結果を DTO に変換し、0 始まりのページを 1 始まりの meta にして返すこと。 */
    @Test
    @SuppressWarnings("unchecked")
    void search_mapsToResponseAndMeta() {
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(
                new PageImpl<>(List.of(category("part", "フレーム")), PageRequest.of(1, 5), 11));
        CategorySearchRequest request = new CategorySearchRequest();
        request.setPage(2);
        request.setSize(5);

        PageResponse<CategoryResponse> result = service.search(request);

        assertEquals(1, result.data().size());
        assertEquals("フレーム", result.data().get(0).categoryName());
        assertEquals(11, result.meta().total());
        assertEquals(2, result.meta().page());
        assertEquals(5, result.meta().size());
        assertEquals(3, result.meta().totalPages());
    }

    /** 既定のソートは、画面仕様どおり「種別の降順、カテゴリ名の降順」で、最後に ID の昇順を付けること。 */
    @Test
    void search_defaultSortIsTypeDescThenNameDescThenId() {
        Pageable pageable = searchAndCapturePageable(new CategorySearchRequest());

        List<Sort.Order> orders = pageable.getSort().toList();
        assertEquals(3, orders.size());
        assertEquals(Sort.Order.desc("categoryType"), orders.get(0));
        assertEquals(Sort.Order.desc("categoryName"), orders.get(1));
        assertEquals(Sort.Order.asc("categoryId"), orders.get(2));
    }

    /** 名前でソートするときは、指定の向きの後に ID の昇順を付けること。 */
    @Test
    void search_sortsByNameThenId() {
        CategorySearchRequest request = new CategorySearchRequest();
        request.setSort("categoryName");
        request.setOrder("ASC");

        List<Sort.Order> orders = searchAndCapturePageable(request).getSort().toList();

        assertEquals(List.of(Sort.Order.asc("categoryName"), Sort.Order.asc("categoryId")), orders);
    }

    /** 許可リストに無いソート項目は、リポジトリを呼ぶ前に BusinessException になること。 */
    @Test
    void search_rejectsSortOutsideTheAllowList() {
        CategorySearchRequest request = new CategorySearchRequest();
        request.setSort("deleteFlag");

        assertThrows(BusinessException.class, () -> service.search(request));
        verifyNoInteractions(repository);
    }

    /** DB に発行できないほど大きいページ番号は、検索せずに空の一覧と総件数を返すこと。 */
    @Test
    @SuppressWarnings("unchecked")
    void search_returnsEmptyPageForHugeOffset() {
        when(repository.count(any(Specification.class))).thenReturn(5L);
        CategorySearchRequest request = new CategorySearchRequest();
        request.setPage(Integer.MAX_VALUE);
        request.setSize(100);

        PageResponse<CategoryResponse> result = service.search(request);

        assertTrue(result.data().isEmpty());
        assertEquals(5, result.meta().total());
        assertEquals(1, result.meta().totalPages());
        verify(repository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    // ---- create ----

    /** 同じ種別に同名が無ければ保存し、保存結果を返すこと。 */
    @Test
    void create_savesNewCategory() {
        when(repository.existsByCategoryTypeAndCategoryNameAndDeleteFlag("part", "フレーム", Category.NOT_DELETED))
                .thenReturn(false);
        when(repository.saveAndFlush(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        CategoryResponse result = service.create(request("part", "フレーム"));

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(repository).saveAndFlush(captor.capture());
        assertEquals("part", captor.getValue().getCategoryType());
        assertEquals("フレーム", captor.getValue().getCategoryName());
        assertEquals(Category.NOT_DELETED, captor.getValue().getDeleteFlag());
        assertEquals("フレーム", result.categoryName());
    }

    /** 同じ種別に同名の未削除のカテゴリがあれば、ConflictException になり保存しないこと。 */
    @Test
    void create_rejectsDuplicateNameInTheSameType() {
        when(repository.existsByCategoryTypeAndCategoryNameAndDeleteFlag("part", "フレーム", Category.NOT_DELETED))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> service.create(request("part", "フレーム")));
        verify(repository, never()).saveAndFlush(any());
    }

    // ---- update ----

    /** 存在しない、または削除済みのカテゴリの更新は NotFoundException になること。 */
    @Test
    void update_throwsNotFoundWhenMissing() {
        when(repository.findByCategoryIdAndDeleteFlag(9, Category.NOT_DELETED)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.update(9, request("part", "x")));
        verify(repository, never()).saveAndFlush(any());
    }

    /** 同じ種別のまま更新するときは、在庫での使用を確認せず、全項目を置き換えること。 */
    @Test
    void update_replacesFieldsWithoutCheckingUsageWhenTypeIsUnchanged() {
        Category existing = category("part", "旧名称");
        when(repository.findByCategoryIdAndDeleteFlag(1, Category.NOT_DELETED)).thenReturn(Optional.of(existing));
        when(repository.existsByCategoryTypeAndCategoryNameAndDeleteFlagAndCategoryIdNot(
                "part", "新名称", Category.NOT_DELETED, 1)).thenReturn(false);
        when(repository.saveAndFlush(existing)).thenReturn(existing);

        CategoryResponse result = service.update(1, new CategoryRequest("part", "新名称", "新しい説明"));

        assertEquals("新名称", existing.getCategoryName());
        assertEquals("新しい説明", existing.getDescription());
        assertEquals("新名称", result.categoryName());
        verifyNoInteractions(partsStockRepository, productsStockRepository);
    }

    /** 他のカテゴリが同じ種別に同名を使っていれば、ConflictException になること。 */
    @Test
    void update_rejectsDuplicateNameOfAnotherCategory() {
        Category existing = category("part", "旧名称");
        when(repository.findByCategoryIdAndDeleteFlag(1, Category.NOT_DELETED)).thenReturn(Optional.of(existing));
        when(repository.existsByCategoryTypeAndCategoryNameAndDeleteFlagAndCategoryIdNot(
                "part", "重複", Category.NOT_DELETED, 1)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.update(1, request("part", "重複")));
        assertEquals("旧名称", existing.getCategoryName());
        verify(repository, never()).saveAndFlush(any());
    }

    /** 在庫で使用中のカテゴリの種別は変更できず、BusinessException になること。 */
    @Test
    void update_rejectsTypeChangeWhileUsedByStock() {
        Category existing = category("part", "フレーム");
        when(repository.findByCategoryIdAndDeleteFlag(1, Category.NOT_DELETED)).thenReturn(Optional.of(existing));
        when(partsStockRepository.existsByCategory_CategoryIdAndDeleteFlag(1, Category.NOT_DELETED))
                .thenReturn(true);

        assertThrows(BusinessException.class, () -> service.update(1, request("product", "フレーム")));
        assertEquals("part", existing.getCategoryType());
        verify(repository, never()).saveAndFlush(any());
    }

    /** 在庫で使用されていなければ、種別を変更できること。 */
    @Test
    void update_allowsTypeChangeWhenUnused() {
        Category existing = category("part", "フレーム");
        when(repository.findByCategoryIdAndDeleteFlag(1, Category.NOT_DELETED)).thenReturn(Optional.of(existing));
        when(partsStockRepository.existsByCategory_CategoryIdAndDeleteFlag(1, Category.NOT_DELETED))
                .thenReturn(false);
        when(productsStockRepository.existsByCategory_CategoryIdAndDeleteFlag(1, Category.NOT_DELETED))
                .thenReturn(false);
        when(repository.saveAndFlush(existing)).thenReturn(existing);

        CategoryResponse result = service.update(1, request("product", "フレーム"));

        assertEquals("product", result.categoryType());
    }

    // ---- delete ----

    /** 存在しないカテゴリの削除は NotFoundException になること。 */
    @Test
    void delete_throwsNotFoundWhenMissing() {
        when(repository.findByCategoryIdAndDeleteFlag(anyInt(), eq(Category.NOT_DELETED)))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.delete(9));
    }

    /** 部品在庫で使用中のカテゴリは削除できないこと。 */
    @Test
    void delete_rejectsCategoryUsedByPartsStock() {
        Category existing = category("part", "フレーム");
        when(repository.findByCategoryIdAndDeleteFlag(1, Category.NOT_DELETED)).thenReturn(Optional.of(existing));
        when(partsStockRepository.existsByCategory_CategoryIdAndDeleteFlag(1, Category.NOT_DELETED))
                .thenReturn(true);

        assertThrows(BusinessException.class, () -> service.delete(1));
        assertEquals(Category.NOT_DELETED, existing.getDeleteFlag());
    }

    /** 製品在庫で使用中のカテゴリは削除できないこと。 */
    @Test
    void delete_rejectsCategoryUsedByProductsStock() {
        Category existing = category("product", "災害用");
        when(repository.findByCategoryIdAndDeleteFlag(11, Category.NOT_DELETED)).thenReturn(Optional.of(existing));
        when(partsStockRepository.existsByCategory_CategoryIdAndDeleteFlag(11, Category.NOT_DELETED))
                .thenReturn(false);
        when(productsStockRepository.existsByCategory_CategoryIdAndDeleteFlag(11, Category.NOT_DELETED))
                .thenReturn(true);

        assertThrows(BusinessException.class, () -> service.delete(11));
        assertEquals(Category.NOT_DELETED, existing.getDeleteFlag());
    }

    /** 使用されていないカテゴリは論理削除（物理削除はしない）し、成功を返すこと。 */
    @Test
    void delete_marksUnusedCategoryAsDeleted() {
        Category existing = category("part", "フレーム");
        when(repository.findByCategoryIdAndDeleteFlag(1, Category.NOT_DELETED)).thenReturn(Optional.of(existing));

        Map<String, String> result = service.delete(1);

        assertEquals(Map.of("result", "success"), result);
        assertEquals(Category.DELETED, existing.getDeleteFlag());
        verify(repository, never()).delete(any(Category.class));
        verify(repository, never()).deleteById(anyInt());
        verify(repository, never()).existsByCategoryTypeAndCategoryNameAndDeleteFlag(anyString(), anyString(), anyInt());
    }
}
