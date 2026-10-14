package com.aojt.mgmt.repository;

import com.aojt.mgmt.entity.InventoryCenter;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InventoryCenterRepository
        extends JpaRepository<InventoryCenter, Integer>, JpaSpecificationExecutor<InventoryCenter> {

    Optional<InventoryCenter> findByCenterIdAndDeleteFlag(Integer centerId, Integer deleteFlag);

    /** Includes logically deleted rows because the DB unique constraint does as well. */
    boolean existsByCenterName(String centerName);

    boolean existsByCenterNameAndCenterIdNot(String centerName, Integer centerId);
}
