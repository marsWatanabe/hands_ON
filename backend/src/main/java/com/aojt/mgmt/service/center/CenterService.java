package com.aojt.mgmt.service.center;

import com.aojt.mgmt.dto.center.CenterRequest;
import com.aojt.mgmt.dto.center.CenterResponse;
import com.aojt.mgmt.dto.center.CenterSearchRequest;
import com.aojt.mgmt.dto.common.PageResponse;
import com.aojt.mgmt.exception.BusinessException;
import com.aojt.mgmt.exception.ConflictException;
import com.aojt.mgmt.exception.NotFoundException;
import com.aojt.mgmt.model.Center;
import com.aojt.mgmt.repository.center.CenterRepository;
import com.aojt.mgmt.repository.center.CenterSpecifications;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 在庫センターの業務ロジック。検索、作成、更新、論理削除を行う。
 */
@Service
public class CenterService {

    /** ソート項目の許可リスト。エンティティの項目名と一致させる。 */
    static final Set<String> SORTABLE_FIELDS = Set.of(
            "centerId", "centerName", "address", "operationalStatus", "maxStorageCapacity",
            "currentStorageCapacity", "createDate", "updateDate");

    /** 同じ値が並んでもページ間で順序が変わらないようにする、第 2 ソートキー。 */
    private static final String TIE_BREAKER = "centerId";

    /** 削除成功時のレスポンスのキーと値。 */
    private static final Map<String, String> DELETE_SUCCESS = Map.of("result", "success");

    private static final String MSG_NAME_EXISTS = "同じ在庫センター名が既に登録されています。";
    private static final String MSG_NOT_FOUND = "在庫センターが見つかりません。ID: ";
    private static final String MSG_INVALID_SORT = "並び替え項目が不正です。";
    private static final String MSG_CAPACITY_EXCEEDED = "現在保管容量は最大保管容量以下にしてください。";

    private final CenterRepository repository;

    /**
     * サービスを生成する。
     *
     * @param repository 在庫センターのリポジトリ
     */
    public CenterService(CenterRepository repository) {
        this.repository = repository;
    }

    /**
     * 在庫センターを検索する。論理削除済みは含まない。
     *
     * @param request 検索条件、ソート、ページング
     * @return 検索結果（{@code data} と {@code meta}）
     * @throws BusinessException ソート項目が許可リストに無い場合
     */
    @Transactional(readOnly = true)
    public PageResponse<CenterResponse> search(CenterSearchRequest request) {
        Page<CenterResponse> page = repository
                .findAll(CenterSpecifications.search(
                        request.getCenterName(), request.getAddress(), request.getOperationalStatus()),
                        toPageable(request))
                .map(CenterResponse::from);
        return PageResponse.of(page);
    }

    /**
     * 在庫センターを新規作成する。
     *
     * @param request 作成内容
     * @return 作成した在庫センター
     * @throws ConflictException 在庫センター名が重複している場合
     * @throws BusinessException 現在保管容量が最大保管容量を超えている場合
     */
    @Transactional
    public CenterResponse create(CenterRequest request) {
        validateCapacity(request);
        if (repository.existsByCenterName(request.centerName())) {
            throw new ConflictException(MSG_NAME_EXISTS);
        }
        Center center = new Center();
        apply(center, request);
        // 重複確認から保存までの間の競合は DB のユニーク制約が検出する（409 になる）。
        return CenterResponse.from(repository.saveAndFlush(center));
    }

    /**
     * 在庫センターを更新する（全項目を置き換える）。
     *
     * @param centerId 在庫センターID
     * @param request 更新内容
     * @return 更新後の在庫センター
     * @throws NotFoundException 在庫センターが存在しない、または削除済みの場合
     * @throws ConflictException 他の在庫センターと名前が重複している場合
     * @throws BusinessException 現在保管容量が最大保管容量を超えている場合
     */
    @Transactional
    public CenterResponse update(Integer centerId, CenterRequest request) {
        validateCapacity(request);
        Center center = findActive(centerId);
        if (repository.existsByCenterNameAndCenterIdNot(request.centerName(), centerId)) {
            throw new ConflictException(MSG_NAME_EXISTS);
        }
        apply(center, request);
        return CenterResponse.from(repository.saveAndFlush(center));
    }

    /**
     * 在庫センターを論理削除する（{@code delete_flag = 1}）。
     *
     * @param centerId 在庫センターID
     * @return {@code {"result": "success"}}
     * @throws NotFoundException 在庫センターが存在しない、または削除済みの場合
     */
    @Transactional
    public Map<String, String> delete(Integer centerId) {
        findActive(centerId).markDeleted();
        return DELETE_SUCCESS;
    }

    /**
     * 未削除の在庫センターを取得する。
     *
     * @param centerId 在庫センターID
     * @return 在庫センター
     * @throws NotFoundException 存在しない、または削除済みの場合
     */
    private Center findActive(Integer centerId) {
        return repository.findByCenterIdAndDeleteFlag(centerId, Center.NOT_DELETED)
                .orElseThrow(() -> new NotFoundException(MSG_NOT_FOUND + centerId));
    }

    /**
     * 現在保管容量が最大保管容量以下であることを確認する（どちらかが未設定なら確認しない）。
     *
     * @param request 作成・更新内容
     * @throws BusinessException 現在保管容量が最大保管容量を超えている場合
     */
    private static void validateCapacity(CenterRequest request) {
        Integer max = request.maxStorageCapacity();
        Integer current = request.currentStorageCapacity();
        if (max != null && current != null && current > max) {
            throw new BusinessException(MSG_CAPACITY_EXCEEDED);
        }
    }

    private static void apply(Center center, CenterRequest r) {
        center.setCenterName(r.centerName());
        center.setPostCode(r.postCode());
        center.setAddress(r.address());
        center.setPhoneNumber(r.phoneNumber());
        center.setManagerName(r.managerName());
        center.setOperationalStatus(r.operationalStatus());
        center.setMaxStorageCapacity(r.maxStorageCapacity());
        center.setCurrentStorageCapacity(r.currentStorageCapacity());
        center.setNotes(r.notes());
    }

    /**
     * ページングとソートを組み立てる。ソート項目は許可リストで検証する。
     *
     * @param request 検索リクエスト
     * @return ページング条件（ページ番号は 0 始まりに変換する）
     * @throws BusinessException ソート項目が許可リストに無い場合
     */
    private static Pageable toPageable(CenterSearchRequest request) {
        if (!SORTABLE_FIELDS.contains(request.getSort())) {
            throw new BusinessException(MSG_INVALID_SORT);
        }
        Sort.Direction direction = Sort.Direction.fromString(request.getOrder());
        Sort sort = Sort.by(direction, request.getSort());
        if (!TIE_BREAKER.equals(request.getSort())) {
            sort = sort.and(Sort.by(Sort.Direction.ASC, TIE_BREAKER));
        }
        return PageRequest.of(request.getPage() - 1, request.getSize(), sort);
    }
}
