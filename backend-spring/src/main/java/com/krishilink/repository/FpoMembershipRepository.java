package com.krishilink.repository;

import com.krishilink.entity.FpoMembership;
import com.krishilink.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FpoMembershipRepository extends JpaRepository<FpoMembership, Long> {
    List<FpoMembership> findByFpo(User fpo);
    Optional<FpoMembership> findByFpoAndFarmer(User fpo, User farmer);
    void deleteByFpoAndFarmer(User fpo, User farmer);
    boolean existsByFpoAndFarmer(User fpo, User farmer);
}
