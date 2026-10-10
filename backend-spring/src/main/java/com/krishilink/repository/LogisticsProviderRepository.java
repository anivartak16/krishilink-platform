package com.krishilink.repository;

import com.krishilink.entity.LogisticsProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogisticsProviderRepository extends JpaRepository<LogisticsProvider, Long> {
    List<LogisticsProvider> findByAvailabilityTrue();
}
