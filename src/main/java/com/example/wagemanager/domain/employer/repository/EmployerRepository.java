package com.example.wagemanager.domain.employer.repository;

import com.example.wagemanager.domain.employer.entity.Employer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployerRepository extends JpaRepository<Employer, Long> {
    @Query("SELECT e FROM Employer e " +
            "JOIN FETCH e.user u " +
            "WHERE u.id = :userId")
    Optional<Employer> findByUserId(@Param("userId") Long userId);
}
