package com.example.wagemanager.domain.salary.repository;

import com.example.wagemanager.domain.salary.entity.Salary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalaryRepository extends JpaRepository<Salary, Long> {
    List<Salary> findByWorkerId(Long workerId);
    List<Salary> findByWorkplaceId(Long workplaceId);
    Optional<Salary> findByWorkerIdAndPaymentDateBetween(Long workerId, LocalDate startDate, LocalDate endDate);
}
