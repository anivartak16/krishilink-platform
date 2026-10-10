package com.krishilink.repository;

import com.krishilink.entity.Demand;
import com.krishilink.entity.User;
import com.krishilink.enums.DemandStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface DemandRepository extends JpaRepository<Demand, Long>, JpaSpecificationExecutor<Demand> {
    List<Demand> findByBuyer(User buyer);
    List<Demand> findByStatusIn(Collection<DemandStatus> statuses);
    List<Demand> findByCommodityIgnoreCaseAndStatus(String commodity, DemandStatus status);
}
