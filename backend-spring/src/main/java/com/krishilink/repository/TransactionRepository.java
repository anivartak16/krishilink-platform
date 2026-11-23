package com.krishilink.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;
import com.krishilink.entity.Transaction;
import com.krishilink.entity.User;
import com.krishilink.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transaction t WHERE t.id = :id")
    Optional<Transaction> findByIdForUpdate(@Param("id") Long id);
    List<Transaction> findByBuyer(User buyer);
    List<Transaction> findBySeller(User seller);

    @Query("SELECT t FROM Transaction t WHERE t.buyer = :user OR t.seller = :user ORDER BY t.createdAt DESC")
    List<Transaction> findByUser(@Param("user") User user);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE (t.buyer = :user OR t.seller = :user) AND t.status = :status")
    long countCompletedByUser(@Param("user") User user, @Param("status") TransactionStatus status);
}
