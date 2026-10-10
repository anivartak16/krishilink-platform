package com.krishilink.repository;

import com.krishilink.entity.User;
import com.krishilink.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<User> findByRole(Role role);

    @Query("select u from User u where u.role = :role and " +
            "(lower(u.name) like lower(concat('%', :search, '%')) or " +
            "lower(u.email) like lower(concat('%', :search, '%')) or " +
            "lower(coalesce(u.location, '')) like lower(concat('%', :search, '%')))")
    List<User> searchByRole(@Param("role") Role role, @Param("search") String search);
}
