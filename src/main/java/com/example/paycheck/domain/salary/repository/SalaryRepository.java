package com.example.paycheck.domain.salary.repository;

import com.example.paycheck.domain.salary.entity.Salary;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalaryRepository extends JpaRepository<Salary, Long> {
    @Query("SELECT DISTINCT s FROM Salary s " +
            "JOIN FETCH s.contract c " +
            "JOIN FETCH c.worker w " +
            "JOIN FETCH w.user u " +
            "WHERE w.id = :workerId " +
            "ORDER BY s.year DESC, s.month DESC")
    List<Salary> findByWorkerId(@Param("workerId") Long workerId);

    @Query("SELECT DISTINCT s FROM Salary s " +
            "JOIN FETCH s.contract c " +
            "JOIN FETCH c.workplace w " +
            "WHERE w.id = :workplaceId " +
            "ORDER BY s.year DESC, s.month DESC")
    List<Salary> findByWorkplaceId(@Param("workplaceId") Long workplaceId);

    @Query("SELECT s FROM Salary s " +
            "JOIN FETCH s.contract c " +
            "WHERE c.id = :contractId " +
            "ORDER BY s.year DESC, s.month DESC")
    List<Salary> findByContractId(@Param("contractId") Long contractId);

    @Query("SELECT s FROM Salary s " +
            "JOIN FETCH s.contract c " +
            "WHERE c.id = :contractId " +
            "AND s.year = :year " +
            "AND s.month = :month")
    List<Salary> findByContractIdAndYearAndMonth(
            @Param("contractId") Long contractId,
            @Param("year") Integer year,
            @Param("month") Integer month
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    @Query("SELECT s FROM Salary s " +
            "JOIN FETCH s.contract c " +
            "WHERE c.id = :contractId " +
            "AND s.year = :year " +
            "AND s.month = :month")
    Optional<Salary> findByContractIdAndYearAndMonthForUpdate(
            @Param("contractId") Long contractId,
            @Param("year") Integer year,
            @Param("month") Integer month
    );
}
