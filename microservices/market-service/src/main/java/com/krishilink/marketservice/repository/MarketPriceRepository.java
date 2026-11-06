package com.krishilink.marketservice.repository;

import com.krishilink.marketservice.entity.MarketPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketPriceRepository extends JpaRepository<MarketPrice, Long>, JpaSpecificationExecutor<MarketPrice> {
    List<MarketPrice> findByCommodityIgnoreCase(String commodity);
    List<MarketPrice> findByStateIgnoreCase(String state);
}
