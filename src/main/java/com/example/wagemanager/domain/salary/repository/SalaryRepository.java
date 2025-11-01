package com.example.wagemanager.domain.salary.repository;

import com.example.wagemanager.domain.salary.entity.Salary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalaryRepository extends JpaRepository<Salary, Long> {
    @Query("SELECT s FROM Salary s WHERE s.contract.worker.id = :workerId")
    List<Salary> findByWorkerId(@Param("workerId") Long workerId);

    @Query("SELECT s FROM Salary s WHERE s.contract.workplace.id = :workplaceId")
    List<Salary> findByWorkplaceId(@Param("workplaceId") Long workplaceId);

    List<Salary> findByContractId(Long contractId);

    @Query("SELECT s FROM Salary s WHERE s.contract.id = :contractId AND s.year = :year AND s.month = :month")
    List<Salary> findByContractIdAndYearAndMonth(@Param("contractId") Long contractId, @Param("year") Integer year, @Param("month") Integer month);
}
