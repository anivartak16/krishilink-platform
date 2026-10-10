package com.krishilink.repository;

import com.krishilink.entity.StorageBooking;
import com.krishilink.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StorageBookingRepository extends JpaRepository<StorageBooking, Long> {
    List<StorageBooking> findByUserOrderByCreatedAtDesc(User user);
}
