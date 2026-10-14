package com.aojt.mgmt.service.center;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aojt.mgmt.dto.center.CenterRequest;
import com.aojt.mgmt.dto.center.CenterResponse;
import com.aojt.mgmt.dto.center.CenterSearchRequest;
import com.aojt.mgmt.dto.common.PageResponse;
import com.aojt.mgmt.exception.BusinessException;
import com.aojt.mgmt.exception.ConflictException;
import com.aojt.mgmt.exception.NotFoundException;
import com.aojt.mgmt.model.Center;
import com.aojt.mgmt.repository.center.CenterRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

/**
 * {@link CenterService} の単体テスト。リポジトリをモックにし、検索条件の組み立て、重複・存在・容量の検証、
 * 論理削除の挙動を確認する。
 */
@ExtendWith(MockitoExtension.class)
class CenterServiceTest {

    @Mock
    CenterRepository repository;

    @InjectMocks
    CenterService service;

    private static CenterRequest request(String name, Integer max, Integer current) {
        return new CenterRequest(name, "530-0001", "大阪府大阪市北区梅田1-1-1", "06-1234-5678", "田中一郎",
                Center.STATUS_OPERATING, max, current, "備考");
    }

    private static Center center(String name) {
        Center c = new Center();
        c.setCenterName(name);
        c.setPostCode("100-0001");
        c.setAddress("東京都");
        c.setPhoneNumber("03-1234-5678");
        c.setManagerName("管理者");
        c.setOperationalStatus(Center.STATUS_OPERATING);
        return c;
    }

    @SuppressWarnings("unchecked")
    private Pageable searchAndCapturePageable(CenterSearchRequest request) {
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
        service.search(request);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), captor.capture());
        return captor.getValue();
    }

    // ---- search ----

    /** 検索結果を DTO に変換し、0 始まりのページを 1 始まりの meta にして返すこと。 */
    @Test
    @SuppressWarnings("unchecked")
    void search_returnsMappedDataAndOneBasedMeta() {
        Page<Center> page = new PageImpl<>(List.of(center("東京センター"), center("大阪センター")),
                PageRequest.of(1, 2), 5);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<CenterResponse> result = service.search(new CenterSearchRequest());

        assertEquals(2, result.data().size());
        assertEquals("東京センター", result.data().get(0).centerName());
        assertEquals(5, result.meta().total());
        assertEquals(2, result.meta().page());
        assertEquals(2, result.meta().size());
        assertEquals(3, result.meta().totalPages());
    }

    /** 未指定の場合は 1 ページ目、20 件、centerId の降順になり、第 2 ソートキーを付けないこと。 */
    @Test
    void search_usesDefaultsWhenNothingIsSpecified() {
        Pageable pageable = searchAndCapturePageable(new CenterSearchRequest());

        assertEquals(0, pageable.getPageNumber());
        assertEquals(20, pageable.getPageSize());
        assertEquals(Sort.by(Sort.Direction.DESC, "centerId"), pageable.getSort());
    }

    /** 指定したページ・件数・ソートを反映し、同値で順序がぶれないよう centerId の昇順を第 2 キーにすること。 */
    @Test
    void search_appliesPageSizeAndSortWithTieBreaker() {
        CenterSearchRequest request = new CenterSearchRequest();
        request.setPage(3);
        request.setSize(10);
        request.setSort("centerName");
        request.setOrder("ASC");

        Pageable pageable = searchAndCapturePageable(request);

        assertEquals(2, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());
        assertEquals(Sort.by(Sort.Direction.ASC, "centerName").and(Sort.by(Sort.Direction.ASC, "centerId")),
                pageable.getSort());
    }

    /** 許可されたソート項目はすべて受け付けること。 */
    @Test
    @SuppressWarnings("unchecked")
    void search_acceptsEverySortableField() {
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
        for (String field : CenterService.SORTABLE_FIELDS) {
            CenterSearchRequest request = new CenterSearchRequest();
            request.setSort(field);
            service.search(request);
        }
    }

    /** 許可リストに無いソート項目は、リポジトリを呼ばずに業務例外にすること。 */
    @Test
    void search_rejectsUnknownSortField() {
        CenterSearchRequest request = new CenterSearchRequest();
        request.setSort("password");

        assertThrows(BusinessException.class, () -> service.search(request));
        verify(repository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    // ---- create ----

    /** 重複が無ければ保存し、保存結果を返すこと。 */
    @Test
    void create_savesAndReturnsTheCenter() {
        when(repository.existsByCenterName("大阪センター")).thenReturn(false);
        when(repository.saveAndFlush(any(Center.class))).thenAnswer(i -> i.getArgument(0));

        CenterResponse response = service.create(request("大阪センター", 800, 100));

        ArgumentCaptor<Center> captor = ArgumentCaptor.forClass(Center.class);
        verify(repository).saveAndFlush(captor.capture());
        Center saved = captor.getValue();
        assertEquals("大阪センター", saved.getCenterName());
        assertEquals("530-0001", saved.getPostCode());
        assertEquals(800, saved.getMaxStorageCapacity());
        assertEquals(100, saved.getCurrentStorageCapacity());
        assertEquals(Center.NOT_DELETED, saved.getDeleteFlag());
        assertEquals("大阪センター", response.centerName());
    }

    /** センター名が重複していれば、保存せずに 409 用の例外にすること。 */
    @Test
    void create_throwsConflictWhenNameExists() {
        when(repository.existsByCenterName("東京センター")).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.create(request("東京センター", 800, 0)));
        verify(repository, never()).saveAndFlush(any(Center.class));
    }

    /** 現在保管容量が最大保管容量を超えれば、業務例外にすること（重複確認より前に判定する）。 */
    @Test
    void create_throwsBusinessExceptionWhenCurrentExceedsMax() {
        assertThrows(BusinessException.class, () -> service.create(request("新規", 100, 101)));
        verify(repository, never()).existsByCenterName(any());
    }

    /** 現在保管容量が最大保管容量と等しい（境界値）場合は受け付けること。 */
    @Test
    void create_acceptsCurrentEqualToMax() {
        when(repository.saveAndFlush(any(Center.class))).thenAnswer(i -> i.getArgument(0));

        CenterResponse response = service.create(request("境界", 100, 100));

        assertEquals(100, response.currentStorageCapacity());
    }

    /** 容量がどちらも未設定でも作成できること。 */
    @Test
    void create_acceptsMissingCapacities() {
        when(repository.saveAndFlush(any(Center.class))).thenAnswer(i -> i.getArgument(0));

        CenterResponse response = service.create(request("容量なし", null, null));

        assertNull(response.maxStorageCapacity());
        assertNull(response.currentStorageCapacity());
    }

    // ---- update ----

    /** 未削除のセンターの全項目を置き換えて保存すること。 */
    @Test
    void update_replacesAllFields() {
        Center existing = center("旧名称");
        when(repository.findByCenterIdAndDeleteFlag(1, Center.NOT_DELETED)).thenReturn(Optional.of(existing));
        when(repository.existsByCenterNameAndCenterIdNot("新名称", 1)).thenReturn(false);
        when(repository.saveAndFlush(existing)).thenReturn(existing);

        CenterResponse response = service.update(1, request("新名称", 900, 10));

        assertEquals("新名称", existing.getCenterName());
        assertEquals("大阪府大阪市北区梅田1-1-1", existing.getAddress());
        assertEquals(900, existing.getMaxStorageCapacity());
        assertEquals("新名称", response.centerName());
    }

    /** 存在しない（または削除済みの）センターは 404 用の例外にすること。 */
    @Test
    void update_throwsNotFoundWhenCenterDoesNotExist() {
        when(repository.findByCenterIdAndDeleteFlag(9, Center.NOT_DELETED)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.update(9, request("名称", 1, 0)));
    }

    /** 他のセンターと名前が重複していれば、保存せずに 409 用の例外にすること。 */
    @Test
    void update_throwsConflictWhenNameBelongsToAnotherCenter() {
        when(repository.findByCenterIdAndDeleteFlag(1, Center.NOT_DELETED))
                .thenReturn(Optional.of(center("旧名称")));
        when(repository.existsByCenterNameAndCenterIdNot("東京センター", 1)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.update(1, request("東京センター", 1, 0)));
        verify(repository, never()).saveAndFlush(any(Center.class));
    }

    /** 更新でも、現在保管容量が最大保管容量を超えれば業務例外にすること。 */
    @Test
    void update_throwsBusinessExceptionWhenCurrentExceedsMax() {
        assertThrows(BusinessException.class, () -> service.update(1, request("名称", 10, 11)));
        verify(repository, never()).findByCenterIdAndDeleteFlag(anyInt(), anyInt());
    }

    // ---- delete ----

    /** 論理削除（削除フラグを立てる）し、物理削除はせずに成功を返すこと。 */
    @Test
    void delete_marksTheCenterDeletedAndReturnsSuccess() {
        Center existing = center("削除対象");
        when(repository.findByCenterIdAndDeleteFlag(eq(1), eq(Center.NOT_DELETED)))
                .thenReturn(Optional.of(existing));

        Map<String, String> result = service.delete(1);

        assertEquals(Center.DELETED, existing.getDeleteFlag());
        assertEquals("success", result.get("result"));
        verify(repository, never()).delete(any(Center.class));
        verify(repository, never()).deleteById(anyInt());
    }

    /** 存在しない（または削除済みの）センターの削除は 404 用の例外にすること。 */
    @Test
    void delete_throwsNotFoundWhenCenterDoesNotExist() {
        when(repository.findByCenterIdAndDeleteFlag(9, Center.NOT_DELETED)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.delete(9));
    }

    /** 許可リストが仕様のソート項目 8 件と一致すること。 */
    @Test
    void sortableFields_matchTheApiSpecification() {
        assertEquals(8, CenterService.SORTABLE_FIELDS.size());
        assertTrue(CenterService.SORTABLE_FIELDS.contains("updateDate"));
    }
}
