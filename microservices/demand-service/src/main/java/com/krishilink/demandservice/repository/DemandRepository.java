package com.krishilink.demandservice.repository;

import com.krishilink.demandservice.entity.Demand;
import com.krishilink.demandservice.enums.DemandStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DemandRepository extends JpaRepository<Demand, Long>, JpaSpecificationExecutor<Demand> {
    List<Demand> findByBuyerId(Long buyerId);
    List<Demand> findByStatus(DemandStatus status);
}
