package com.krishilink.tradeservice.repository;

import com.krishilink.tradeservice.entity.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {
    List<Dispute> findByRaisedById(Long raisedById);
}
