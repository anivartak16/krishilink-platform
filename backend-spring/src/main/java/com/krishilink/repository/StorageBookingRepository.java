package com.krishilink.repository;

import com.krishilink.entity.StorageBooking;
import com.krishilink.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StorageBookingRepository extends JpaRepository<StorageBooking, Long> {
    List<StorageBooking> findByUserOrderByCreatedAtDesc(User user);
}
