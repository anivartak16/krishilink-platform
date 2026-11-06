package com.krishilink.marketservice.repository;

import com.krishilink.marketservice.entity.LogisticsBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogisticsBookingRepository extends JpaRepository<LogisticsBooking, Long> {
    List<LogisticsBooking> findByUserId(Long userId);
}
