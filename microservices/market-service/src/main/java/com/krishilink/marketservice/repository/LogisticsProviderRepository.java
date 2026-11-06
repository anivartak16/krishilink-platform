package com.krishilink.marketservice.repository;

import com.krishilink.marketservice.entity.LogisticsProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogisticsProviderRepository extends JpaRepository<LogisticsProvider, Long> {
}
