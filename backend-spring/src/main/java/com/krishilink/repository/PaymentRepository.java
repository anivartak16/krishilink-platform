package com.krishilink.repository;

import com.krishilink.entity.Payment;
import com.krishilink.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTransaction(Transaction transaction);
    Optional<Payment> findByTransactionId(Long transactionId);
}
