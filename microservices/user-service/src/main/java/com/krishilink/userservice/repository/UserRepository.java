package com.krishilink.userservice.repository;

import com.krishilink.userservice.entity.User;
import com.krishilink.userservice.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<User> findByRole(Role role);
    List<User> findByRoleAndNameContainingIgnoreCaseOrRoleAndLocationContainingIgnoreCase(
            Role r1, String name, Role r2, String location);
}
