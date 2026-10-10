package com.krishilink.repository;

import com.krishilink.entity.Lot;
import com.krishilink.entity.User;
import com.krishilink.enums.LotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface LotRepository extends JpaRepository<Lot, Long>, JpaSpecificationExecutor<Lot> {
    List<Lot> findByOwner(User owner);
    List<Lot> findByStatusIn(Collection<LotStatus> statuses);
    List<Lot> findByCommodityIgnoreCaseAndStatusIn(String commodity, Collection<LotStatus> statuses);
}
