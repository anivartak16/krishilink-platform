package com.krishilink.marketservice.repository;

import com.krishilink.marketservice.entity.StorageBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StorageBookingRepository extends JpaRepository<StorageBooking, Long> {
    List<StorageBooking> findByUserId(Long userId);
}
