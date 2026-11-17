package com.krishilink.repository;

import com.krishilink.entity.MspBenchmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MspBenchmarkRepository extends JpaRepository<MspBenchmark, Long> {
    Optional<MspBenchmark> findByCommodityIgnoreCase(String commodity);
    List<MspBenchmark> findAllByOrderByCommodityAsc();
}
