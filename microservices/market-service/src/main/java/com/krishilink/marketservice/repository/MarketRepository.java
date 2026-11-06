package com.krishilink.marketservice.repository;

import com.krishilink.marketservice.entity.Market;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketRepository extends JpaRepository<Market, Long> {
    List<Market> findByDistrictIgnoreCase(String district);
    List<Market> findByStateIgnoreCase(String state);
}
