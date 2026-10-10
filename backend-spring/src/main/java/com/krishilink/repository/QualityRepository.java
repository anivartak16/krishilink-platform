package com.krishilink.repository;

import com.krishilink.entity.Quality;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QualityRepository extends JpaRepository<Quality, Long> {
    Optional<Quality> findByGradeIgnoreCase(String grade);
}
