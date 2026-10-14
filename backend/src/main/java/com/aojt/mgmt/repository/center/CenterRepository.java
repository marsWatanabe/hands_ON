package com.aojt.mgmt.repository.center;

import com.aojt.mgmt.model.Center;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 在庫センターのリポジトリ。検索条件は {@link CenterSpecifications} で組み立てる。
 */
public interface CenterRepository extends JpaRepository<Center, Integer>, JpaSpecificationExecutor<Center> {

    /**
     * 削除フラグを指定して、在庫センターを検索する。
     *
     * @param centerId 在庫センターID
     * @param deleteFlag 削除フラグ（{@link Center#NOT_DELETED} または {@link Center#DELETED}）
     * @return 該当する在庫センター
     */
    Optional<Center> findByCenterIdAndDeleteFlag(Integer centerId, Integer deleteFlag);

    /**
     * 在庫センター名が使われているかを確認する。DB のユニーク制約と同じく、論理削除済みも対象にする。
     *
     * @param centerName 在庫センター名
     * @return 使われていれば true
     */
    boolean existsByCenterName(String centerName);

    /**
     * 指定した在庫センター以外で、在庫センター名が使われているかを確認する。論理削除済みも対象にする。
     *
     * @param centerName 在庫センター名
     * @param centerId 除外する在庫センターID
     * @return 使われていれば true
     */
    boolean existsByCenterNameAndCenterIdNot(String centerName, Integer centerId);
}
