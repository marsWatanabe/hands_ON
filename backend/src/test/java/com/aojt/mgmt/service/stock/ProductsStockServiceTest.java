package com.aojt.mgmt.service.stock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
 * {@link ProductsStockService} の単体テスト。部品在庫に加えて、製品コード（削除済みを含めて一意）の重複、
 * 製品コードでの検索・ソートを確認する。
 */
@ExtendWith(MockitoExtension.class)
class ProductsStockServiceTest {

    @Mock
    ProductsStockRepository repository;

    @Mock
    StockReferenceResolver references;

    @InjectMocks
    ProductsStockService service;

    private static Category category() {
        Category c = new Category();
        c.setCategoryType(Category.TYPE_PRODUCT);
        c.setCategoryName("災害用ドローン");
        return c;
    }

    private static Center center() {
        Center c = new Center();
        c.setCenterName("大阪センター");
        return c;
    }

    private static ProductsStock stock(String code, String name) {
        ProductsStock s = new ProductsStock();
        s.setProductCode(code);
        s.setCategory(category());
        s.setCenter(center());
        s.setName(name);
        s.setAmount(3);
        return s;
    }

    private static ProductsStockRequest request(String code, String name) {
        return new ProductsStockRequest(code, 11, name, 2, "説明", 12);
    }

    @SuppressWarnings("unchecked")
    private Pageable searchAndCapturePageable(ProductsStockSearchRequest request) {
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        service.search(request);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), captor.capture());
        return captor.getValue();
    }

    // ---- search ----

    /** 製品コードとカテゴリ名、センター名を含む DTO に変換して返すこと。 */
    @Test
    @SuppressWarnings("unchecked")
    void search_mapsToResponseWithProductCodeAndJoinedNames() {
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(
                new PageImpl<>(List.of(stock("PRD-000001", "災害用ドローン-X")), PageRequest.of(0, 20), 1));

        PageResponse<ProductsStockResponse> result = service.search(new ProductsStockSearchRequest());

        ProductsStockResponse r = result.data().get(0);
        assertEquals("PRD-000001", r.productCode());
        assertEquals("災害用ドローン", r.categoryName());
        assertEquals("大阪センター", r.centerName());
        assertEquals(1, result.meta().total());
    }

    /** 製品コードでソートでき、第 2 キーに在庫 ID の昇順を付けること。 */
    @Test
    void search_sortsByProductCode() {
        ProductsStockSearchRequest request = new ProductsStockSearchRequest();
        request.setSort("productCode");
        request.setOrder("desc");

        List<Sort.Order> orders = searchAndCapturePageable(request).getSort().toList();

        assertEquals(List.of(Sort.Order.desc("productCode"), Sort.Order.asc("stockId")), orders);
    }

    /** 既定のソートは「カテゴリ名、名称」の昇順であること。 */
    @Test
    void search_defaultSortIsCategoryNameThenName() {
        List<Sort.Order> orders = searchAndCapturePageable(new ProductsStockSearchRequest()).getSort().toList();

        assertEquals(List.of(
                Sort.Order.asc("category.categoryName"), Sort.Order.asc("name"), Sort.Order.asc("stockId")), orders);
    }

    /** 許可リストに無いソート項目は BusinessException になること。 */
    @Test
    void search_rejectsUnknownSort() {
        ProductsStockSearchRequest request = new ProductsStockSearchRequest();
        request.setSort("deleteFlag");

        assertThrows(BusinessException.class, () -> service.search(request));
        verifyNoInteractions(repository);
    }

    /** 個数（以上）が個数（以下）を超える指定は BusinessException になること。 */
    @Test
    void search_rejectsReversedAmountRange() {
        ProductsStockSearchRequest request = new ProductsStockSearchRequest();
        request.setAmountMin(10);
        request.setAmountMax(9);

        assertThrows(BusinessException.class, () -> service.search(request));
    }

    /** 製品コードを指定した検索も実行できること（条件はリポジトリに渡される）。 */
    @Test
    @SuppressWarnings("unchecked")
    void search_acceptsProductCodeCondition() {
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        ProductsStockSearchRequest request = new ProductsStockSearchRequest();
        request.setProductCode("PRD");

        assertTrue(service.search(request).data().isEmpty());
        verify(repository).findAll(any(Specification.class), any(Pageable.class));
    }

    /** DB に発行できないほど大きいページ番号は、検索せずに空の一覧を返すこと。 */
    @Test
    @SuppressWarnings("unchecked")
    void search_returnsEmptyPageForHugeOffset() {
        when(repository.count(any(Specification.class))).thenReturn(2L);
        ProductsStockSearchRequest request = new ProductsStockSearchRequest();
        request.setPage(Integer.MAX_VALUE);

        assertTrue(service.search(request).data().isEmpty());
        verify(repository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    // ---- create ----

    /** 参照先を検証し、製品コードとセンター内の名称が重複しなければ保存すること。 */
    @Test
    void create_savesStock() {
        when(references.requireCategory(11, Category.TYPE_PRODUCT)).thenReturn(category());
        when(references.requireCenter(2)).thenReturn(center());
        when(repository.existsByProductCode("PRD-X1")).thenReturn(false);
        when(repository.existsByCenter_CenterIdAndNameAndDeleteFlag(2, "新製品", StockItem.NOT_DELETED))
                .thenReturn(false);
        when(repository.saveAndFlush(any(ProductsStock.class))).thenAnswer(i -> i.getArgument(0));

        ProductsStockResponse result = service.create(request("PRD-X1", "新製品"));

        ArgumentCaptor<ProductsStock> captor = ArgumentCaptor.forClass(ProductsStock.class);
        verify(repository).saveAndFlush(captor.capture());
        assertEquals("PRD-X1", captor.getValue().getProductCode());
        assertEquals(12, captor.getValue().getAmount());
        assertEquals("PRD-X1", result.productCode());
    }

    /** 製品コードが（削除済みを含めて）使われていれば ConflictException になること。 */
    @Test
    void create_rejectsDuplicateProductCode() {
        when(references.requireCategory(11, Category.TYPE_PRODUCT)).thenReturn(category());
        when(references.requireCenter(2)).thenReturn(center());
        when(repository.existsByProductCode("PRD-DUP")).thenReturn(true);

        ConflictException e = assertThrows(ConflictException.class, () -> service.create(request("PRD-DUP", "x")));
        assertTrue(e.getMessage().contains("製品コード"));
        verify(repository, never()).saveAndFlush(any());
    }

    /** 同じセンターに同名の未削除の製品があれば ConflictException になること。 */
    @Test
    void create_rejectsDuplicateNameInTheSameCenter() {
        when(references.requireCategory(11, Category.TYPE_PRODUCT)).thenReturn(category());
        when(references.requireCenter(2)).thenReturn(center());
        when(repository.existsByProductCode("PRD-X2")).thenReturn(false);
        when(repository.existsByCenter_CenterIdAndNameAndDeleteFlag(2, "重複", StockItem.NOT_DELETED))
                .thenReturn(true);

        ConflictException e = assertThrows(ConflictException.class, () -> service.create(request("PRD-X2", "重複")));
        assertTrue(e.getMessage().contains("名称"));
        verify(repository, never()).saveAndFlush(any());
    }

    /** カテゴリが製品用でないときは、保存せずに BusinessException を伝えること。 */
    @Test
    void create_propagatesInvalidCategory() {
        when(references.requireCategory(11, Category.TYPE_PRODUCT)).thenThrow(new BusinessException("種別が違います"));

        assertThrows(BusinessException.class, () -> service.create(request("PRD-X3", "x")));
        verify(repository, never()).saveAndFlush(any());
    }

    // ---- update ----

    /** 存在しない、または削除済みの製品在庫の更新は NotFoundException になること。 */
    @Test
    void update_throwsNotFoundWhenMissing() {
        when(repository.findByStockIdAndDeleteFlag(9, StockItem.NOT_DELETED)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.update(9, request("PRD-X", "x")));
    }

    /** 自分以外が製品コードを使っていなければ、全項目を置き換えて保存すること。 */
    @Test
    void update_replacesAllFields() {
        ProductsStock existing = stock("PRD-OLD", "旧名称");
        when(repository.findByStockIdAndDeleteFlag(1, StockItem.NOT_DELETED)).thenReturn(Optional.of(existing));
        when(references.requireCategory(11, Category.TYPE_PRODUCT)).thenReturn(category());
        when(references.requireCenter(2)).thenReturn(center());
        when(repository.existsByProductCodeAndStockIdNot("PRD-NEW", 1)).thenReturn(false);
        when(repository.existsByCenter_CenterIdAndNameAndDeleteFlagAndStockIdNot(
                2, "新名称", StockItem.NOT_DELETED, 1)).thenReturn(false);
        when(repository.saveAndFlush(existing)).thenReturn(existing);

        ProductsStockResponse result = service.update(1, request("PRD-NEW", "新名称"));

        assertEquals("PRD-NEW", existing.getProductCode());
        assertEquals("新名称", existing.getName());
        assertEquals("PRD-NEW", result.productCode());
    }

    /** 他の製品在庫が同じ製品コードを使っていれば ConflictException になり、既存の値を変えないこと。 */
    @Test
    void update_rejectsProductCodeOfAnotherStock() {
        ProductsStock existing = stock("PRD-OLD", "旧名称");
        when(repository.findByStockIdAndDeleteFlag(1, StockItem.NOT_DELETED)).thenReturn(Optional.of(existing));
        when(references.requireCategory(11, Category.TYPE_PRODUCT)).thenReturn(category());
        when(references.requireCenter(2)).thenReturn(center());
        when(repository.existsByProductCodeAndStockIdNot("PRD-USED", 1)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.update(1, request("PRD-USED", "x")));
        assertEquals("PRD-OLD", existing.getProductCode());
        verify(repository, never()).saveAndFlush(any());
    }

    /** 他の製品在庫が同じセンターで同名なら ConflictException になること。 */
    @Test
    void update_rejectsDuplicateNameInTheSameCenter() {
        ProductsStock existing = stock("PRD-OLD", "旧名称");
        when(repository.findByStockIdAndDeleteFlag(1, StockItem.NOT_DELETED)).thenReturn(Optional.of(existing));
        when(references.requireCategory(11, Category.TYPE_PRODUCT)).thenReturn(category());
        when(references.requireCenter(2)).thenReturn(center());
        when(repository.existsByProductCodeAndStockIdNot("PRD-OLD", 1)).thenReturn(false);
        when(repository.existsByCenter_CenterIdAndNameAndDeleteFlagAndStockIdNot(
                2, "重複", StockItem.NOT_DELETED, 1)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.update(1, request("PRD-OLD", "重複")));
        assertEquals("旧名称", existing.getName());
    }

    // ---- delete ----

    /** 存在しない製品在庫の削除は NotFoundException になること。 */
    @Test
    void delete_throwsNotFoundWhenMissing() {
        when(repository.findByStockIdAndDeleteFlag(anyInt(), any())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.delete(9));
    }

    /** 論理削除（物理削除はしない）し、成功を返すこと。製品コードは残ること。 */
    @Test
    void delete_marksAsDeletedAndKeepsTheProductCode() {
        ProductsStock existing = stock("PRD-DEL", "削除対象");
        when(repository.findByStockIdAndDeleteFlag(1, StockItem.NOT_DELETED)).thenReturn(Optional.of(existing));

        Map<String, String> result = service.delete(1);

        assertEquals(Map.of("result", "success"), result);
        assertEquals(StockItem.DELETED, existing.getDeleteFlag());
        assertEquals("PRD-DEL", existing.getProductCode());
        verify(repository, never()).delete(any(ProductsStock.class));
    }
}
