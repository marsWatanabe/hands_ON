package com.aojt.mgmt.service;

import com.aojt.mgmt.dto.CenterRequest;
import com.aojt.mgmt.dto.CenterResponse;
import com.aojt.mgmt.dto.CenterSearchRequest;
import com.aojt.mgmt.dto.PageResponse;
import com.aojt.mgmt.entity.InventoryCenter;
import com.aojt.mgmt.exception.ConflictException;
import com.aojt.mgmt.exception.ResourceNotFoundException;
import com.aojt.mgmt.repository.InventoryCenterRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryCenterService {

    private static final char LIKE_ESCAPE = '\\';

    private final InventoryCenterRepository repository;

    public InventoryCenterService(InventoryCenterRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PageResponse<CenterResponse> search(CenterSearchRequest request) {
        Page<CenterResponse> page = repository
                .findAll(toSpecification(request), toPageable(request))
                .map(CenterResponse::from);
        return PageResponse.of(page);
    }

    @Transactional
    public CenterResponse create(CenterRequest request) {
        if (repository.existsByCenterName(request.centerName())) {
            throw new ConflictException("Center name already exists");
        }
        InventoryCenter center = new InventoryCenter();
        apply(center, request);
        return CenterResponse.from(repository.saveAndFlush(center));
    }

    @Transactional
    public CenterResponse update(Integer centerId, CenterRequest request) {
        InventoryCenter center = findActive(centerId);
        if (repository.existsByCenterNameAndCenterIdNot(request.centerName(), centerId)) {
            throw new ConflictException("Center name already exists");
        }
        apply(center, request);
        return CenterResponse.from(repository.saveAndFlush(center));
    }

    @Transactional
    public Map<String, String> delete(Integer centerId) {
        findActive(centerId).markDeleted();
        return Map.of("result", "success");
    }

    private InventoryCenter findActive(Integer centerId) {
        return repository.findByCenterIdAndDeleteFlag(centerId, InventoryCenter.NOT_DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory center not found: " + centerId));
    }

    private static void apply(InventoryCenter center, CenterRequest r) {
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

    private static Specification<InventoryCenter> toSpecification(CenterSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleteFlag"), InventoryCenter.NOT_DELETED));
            if (hasText(request.getCenterName())) {
                predicates.add(contains(cb, root.get("centerName"), request.getCenterName()));
            }
            if (hasText(request.getAddress())) {
                predicates.add(contains(cb, root.get("address"), request.getAddress()));
            }
            if (request.getOperationalStatus() != null) {
                predicates.add(cb.equal(root.get("operationalStatus"), request.getOperationalStatus()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Predicate contains(jakarta.persistence.criteria.CriteriaBuilder cb,
            jakarta.persistence.criteria.Expression<String> field, String value) {
        String escaped = value.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return cb.like(cb.lower(field), "%" + escaped + "%", LIKE_ESCAPE);
    }

    /** Sort values are whitelisted by {@link CenterSearchRequest}; centerId is a stable tie-breaker. */
    private static Pageable toPageable(CenterSearchRequest request) {
        Sort.Direction direction = Sort.Direction.fromString(request.getOrder());
        Sort sort = Sort.by(direction, request.getSort());
        if (!"centerId".equals(request.getSort())) {
            sort = sort.and(Sort.by(Sort.Direction.ASC, "centerId"));
        }
        return PageRequest.of(request.getPage() - 1, request.getSize(), sort);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
