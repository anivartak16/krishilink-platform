package com.krishilink.repository;

import com.krishilink.entity.MarketPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketPriceRepository extends JpaRepository<MarketPrice, Long> {
    List<MarketPrice> findByCommodityIgnoreCaseOrderByPriceDateAsc(String commodity);
    List<MarketPrice> findByCommodityIgnoreCaseOrderByPriceDateDesc(String commodity);

    @Query("SELECT mp FROM MarketPrice mp WHERE LOWER(mp.commodity) LIKE LOWER(CONCAT('%', :commodity, '%')) ORDER BY mp.priceDate DESC")
    List<MarketPrice> searchByCommodity(@Param("commodity") String commodity);

    @Query("SELECT DISTINCT mp.commodity FROM MarketPrice mp")
    List<String> findDistinctCommodities();
}
