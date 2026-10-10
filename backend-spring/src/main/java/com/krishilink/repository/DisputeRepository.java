package com.krishilink.repository;

import com.krishilink.entity.Dispute;
import com.krishilink.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {
    List<Dispute> findByRaisedBy(User raisedBy);
    List<Dispute> findByAgainst(User against);
    List<Dispute> findByStatus(String status);
}
