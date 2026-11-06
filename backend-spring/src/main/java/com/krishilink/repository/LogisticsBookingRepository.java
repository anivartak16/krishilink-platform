package com.krishilink.repository;

import com.krishilink.entity.LogisticsBooking;
import com.krishilink.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogisticsBookingRepository extends JpaRepository<LogisticsBooking, Long> {
    List<LogisticsBooking> findByUserOrderByCreatedAtDesc(User user);
}
