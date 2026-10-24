package com.aojt.mgmt.service.stock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
 * {@link PartsStockService} の単体テスト。リポジトリとカテゴリ・センターの検証をモックにし、検索のソート、
 * 個数の範囲、センター内の名称重複、参照先の検証、論理削除の挙動を確認する。
 */
@ExtendWith(MockitoExtension.class)
class PartsStockServiceTest {

    @Mock
    PartsStockRepository repository;

    @Mock
    StockReferenceResolver references;

    @InjectMocks
    PartsStockService service;

    private static Category category() {
        Category c = new Category();
        c.setCategoryType(Category.TYPE_PART);
        c.setCategoryName("フレーム");
        return c;
    }

    private static Center center() {
        Center c = new Center();
        c.setCenterName("東京センター");
        return c;
    }

    private static PartsStock stock(String name) {
        PartsStock s = new PartsStock();
        s.setCategory(category());
        s.setCenter(center());
        s.setName(name);
        s.setAmount(10);
        return s;
    }

    private static PartsStockRequest request(String name) {
        return new PartsStockRequest(1, name, 2, "説明", 85);
    }

    @SuppressWarnings("unchecked")
    private Pageable searchAndCapturePageable(StockSearchRequest request) {
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        service.search(request);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), captor.capture());
        return captor.getValue();
    }

    // ---- search ----

    /** カテゴリ名とセンター名を含む DTO に変換し、meta を 1 始まりで返すこと。 */
    @Test
    @SuppressWarnings("unchecked")
    void search_mapsToResponseWithJoinedNamesAndMeta() {
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(
                new PageImpl<>(List.of(stock("カーボンフレーム")), PageRequest.of(0, 20), 1));

        PageResponse<PartsStockResponse> result = service.search(new StockSearchRequest());

        assertEquals(1, result.data().size());
        PartsStockResponse r = result.data().get(0);
        assertEquals("カーボンフレーム", r.name());
        assertEquals("フレーム", r.categoryName());
        assertEquals("東京センター", r.centerName());
        assertEquals(1, result.meta().total());
        assertEquals(1, result.meta().page());
    }

    /** カテゴリ・センターが未設定の既存行でも、名称を null として返せること。 */
    @Test
    @SuppressWarnings("unchecked")
    void search_returnsNullNamesWhenReferencesAreMissing() {
        PartsStock orphan = new PartsStock();
        orphan.setName("孤立した在庫");
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(orphan)));

        PartsStockResponse r = service.search(new StockSearchRequest()).data().get(0);

        assertNull(r.categoryId());
        assertNull(r.categoryName());
        assertNull(r.centerId());
        assertNull(r.centerName());
    }

    /** 既定のソートは「カテゴリ名、名称」の昇順で、最後に在庫 ID の昇順を付けること。 */
    @Test
    void search_defaultSortIsCategoryNameThenNameThenId() {
        List<Sort.Order> orders = searchAndCapturePageable(new StockSearchRequest()).getSort().toList();

        assertEquals(List.of(
                Sort.Order.asc("category.categoryName"), Sort.Order.asc("name"), Sort.Order.asc("stockId")), orders);
    }

    /** センター名でのソートは、結合先の項目で並べること。 */
    @Test
    void search_sortsByJoinedCenterName() {
        StockSearchRequest request = new StockSearchRequest();
        request.setSort("centerName");
        request.setOrder("desc");

        List<Sort.Order> orders = searchAndCapturePageable(request).getSort().toList();

        assertEquals(List.of(Sort.Order.desc("center.centerName"), Sort.Order.asc("stockId")), orders);
    }

    /** 在庫 ID でのソートは、第 2 キーを付けないこと。 */
    @Test
    void search_sortByStockIdHasNoTieBreaker() {
        StockSearchRequest request = new StockSearchRequest();
        request.setSort("stockId");

        assertEquals(List.of(Sort.Order.asc("stockId")), searchAndCapturePageable(request).getSort().toList());
    }

    /** 製品コードは部品在庫のソート項目に無いため、BusinessException になること。 */
    @Test
    void search_rejectsProductCodeSort() {
        StockSearchRequest request = new StockSearchRequest();
        request.setSort("productCode");

        assertThrows(BusinessException.class, () -> service.search(request));
        verifyNoInteractions(repository);
    }

    /** 個数（以上）が個数（以下）を超える指定は BusinessException になること。 */
    @Test
    void search_rejectsReversedAmountRange() {
        StockSearchRequest request = new StockSearchRequest();
        request.setAmountMin(10);
        request.setAmountMax(5);

        assertThrows(BusinessException.class, () -> service.search(request));
        verifyNoInteractions(repository);
    }

    /** 個数の上下限が同じ値（境界）なら検索できること。 */
    @Test
    @SuppressWarnings("unchecked")
    void search_acceptsEqualAmountBounds() {
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        StockSearchRequest request = new StockSearchRequest();
        request.setAmountMin(5);
        request.setAmountMax(5);

        assertTrue(service.search(request).data().isEmpty());
    }

    /** DB に発行できないほど大きいページ番号は、検索せずに空の一覧を返すこと。 */
    @Test
    @SuppressWarnings("unchecked")
    void search_returnsEmptyPageForHugeOffset() {
        when(repository.count(any(Specification.class))).thenReturn(3L);
        StockSearchRequest request = new StockSearchRequest();
        request.setPage(Integer.MAX_VALUE);

        PageResponse<PartsStockResponse> result = service.search(request);

        assertTrue(result.data().isEmpty());
        assertEquals(3, result.meta().total());
        verify(repository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    // ---- create ----

    /** 参照先を検証し、センター内に同名が無ければ保存して、結合した名称を含む結果を返すこと。 */
    @Test
    void create_savesStockWithValidatedReferences() {
        Category category = category();
        Center center = center();
        when(references.requireCategory(1, Category.TYPE_PART)).thenReturn(category);
        when(references.requireCenter(2)).thenReturn(center);
        when(repository.existsByCenter_CenterIdAndNameAndDeleteFlag(2, "新規部品", StockItem.NOT_DELETED))
                .thenReturn(false);
        when(repository.saveAndFlush(any(PartsStock.class))).thenAnswer(i -> i.getArgument(0));

        PartsStockResponse result = service.create(request("新規部品"));

        ArgumentCaptor<PartsStock> captor = ArgumentCaptor.forClass(PartsStock.class);
        verify(repository).saveAndFlush(captor.capture());
        assertEquals(category, captor.getValue().getCategory());
        assertEquals(center, captor.getValue().getCenter());
        assertEquals(85, captor.getValue().getAmount());
        assertEquals(StockItem.NOT_DELETED, captor.getValue().getDeleteFlag());
        assertEquals("フレーム", result.categoryName());
        assertEquals("東京センター", result.centerName());
    }

    /** カテゴリが存在しない・部品用でないときは、保存せずに BusinessException を伝えること。 */
    @Test
    void create_propagatesInvalidCategory() {
        when(references.requireCategory(1, Category.TYPE_PART)).thenThrow(new BusinessException("カテゴリが見つかりません。"));

        assertThrows(BusinessException.class, () -> service.create(request("x")));
        verify(repository, never()).saveAndFlush(any());
    }

    /** センターが存在しないときは、保存せずに BusinessException を伝えること。 */
    @Test
    void create_propagatesInvalidCenter() {
        when(references.requireCategory(1, Category.TYPE_PART)).thenReturn(category());
        when(references.requireCenter(2)).thenThrow(new BusinessException("在庫センターが見つかりません。"));

        assertThrows(BusinessException.class, () -> service.create(request("x")));
        verify(repository, never()).saveAndFlush(any());
    }

    /** 同じセンターに同名の未削除の在庫があれば、ConflictException になり保存しないこと。 */
    @Test
    void create_rejectsDuplicateNameInTheSameCenter() {
        when(references.requireCategory(1, Category.TYPE_PART)).thenReturn(category());
        when(references.requireCenter(2)).thenReturn(center());
        when(repository.existsByCenter_CenterIdAndNameAndDeleteFlag(2, "重複", StockItem.NOT_DELETED))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> service.create(request("重複")));
        verify(repository, never()).saveAndFlush(any());
    }

    // ---- update ----

    /** 存在しない、または削除済みの在庫の更新は NotFoundException になること。 */
    @Test
    void update_throwsNotFoundWhenMissing() {
        when(repository.findByStockIdAndDeleteFlag(9, StockItem.NOT_DELETED)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.update(9, request("x")));
        verifyNoInteractions(references);
    }

    /** 自分以外に同名が無ければ、全項目を置き換えて保存すること。 */
    @Test
    void update_replacesAllFields() {
        PartsStock existing = stock("旧名称");
        Category newCategory = category();
        Center newCenter = center();
        when(repository.findByStockIdAndDeleteFlag(1, StockItem.NOT_DELETED)).thenReturn(Optional.of(existing));
        when(references.requireCategory(1, Category.TYPE_PART)).thenReturn(newCategory);
        when(references.requireCenter(2)).thenReturn(newCenter);
        when(repository.existsByCenter_CenterIdAndNameAndDeleteFlagAndStockIdNot(
                2, "新名称", StockItem.NOT_DELETED, 1)).thenReturn(false);
        when(repository.saveAndFlush(existing)).thenReturn(existing);

        PartsStockResponse result = service.update(1, request("新名称"));

        assertEquals("新名称", existing.getName());
        assertEquals("説明", existing.getDescription());
        assertEquals(85, existing.getAmount());
        assertEquals(newCategory, existing.getCategory());
        assertEquals(newCenter, existing.getCenter());
        assertEquals("新名称", result.name());
    }

    /** 他の在庫が同じセンターで同名なら ConflictException になり、既存の値を変えないこと。 */
    @Test
    void update_rejectsDuplicateNameAndKeepsTheEntityUnchanged() {
        PartsStock existing = stock("旧名称");
        when(repository.findByStockIdAndDeleteFlag(1, StockItem.NOT_DELETED)).thenReturn(Optional.of(existing));
        when(references.requireCategory(1, Category.TYPE_PART)).thenReturn(category());
        when(references.requireCenter(2)).thenReturn(center());
        when(repository.existsByCenter_CenterIdAndNameAndDeleteFlagAndStockIdNot(
                2, "重複", StockItem.NOT_DELETED, 1)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.update(1, request("重複")));
        assertEquals("旧名称", existing.getName());
        verify(repository, never()).saveAndFlush(any());
    }

    // ---- delete ----

    /** 存在しない在庫の削除は NotFoundException になること。 */
    @Test
    void delete_throwsNotFoundWhenMissing() {
        when(repository.findByStockIdAndDeleteFlag(anyInt(), any())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.delete(9));
    }

    /** 論理削除（物理削除はしない）し、成功を返すこと。 */
    @Test
    void delete_marksAsDeleted() {
        PartsStock existing = stock("削除対象");
        when(repository.findByStockIdAndDeleteFlag(1, StockItem.NOT_DELETED)).thenReturn(Optional.of(existing));

        Map<String, String> result = service.delete(1);

        assertEquals(Map.of("result", "success"), result);
        assertEquals(StockItem.DELETED, existing.getDeleteFlag());
        verify(repository, never()).delete(any(PartsStock.class));
    }
}
