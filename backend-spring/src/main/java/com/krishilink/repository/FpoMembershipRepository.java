package com.krishilink.repository;

import com.krishilink.entity.FpoMembership;
import com.krishilink.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FpoMembershipRepository extends JpaRepository<FpoMembership, Long> {
    List<FpoMembership> findByFpoOrderByCreatedAtDesc(User fpo);
    Optional<FpoMembership> findByFpoAndFarmer(User fpo, User farmer);
    Optional<FpoMembership> findByFpoAndFarmerId(User fpo, Long farmerId);
    boolean existsByFpoAndFarmer(User fpo, User farmer);
}
