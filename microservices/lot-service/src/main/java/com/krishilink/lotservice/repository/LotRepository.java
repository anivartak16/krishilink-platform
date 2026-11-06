package com.krishilink.lotservice.repository;

import com.krishilink.lotservice.entity.Lot;
import com.krishilink.lotservice.enums.LotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface LotRepository extends JpaRepository<Lot, Long>, JpaSpecificationExecutor<Lot> {
    List<Lot> findByCommodityIgnoreCaseAndStatusIn(String commodity, List<LotStatus> statuses);
    List<Lot> findByOwnerId(Long ownerId);
    List<Lot> findByOwnerIdInAndStatusIn(Collection<Long> ownerIds, Collection<LotStatus> statuses);
}
