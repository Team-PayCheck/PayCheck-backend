package com.example.wagemanager.domain.payment.repository;

import com.example.wagemanager.domain.payment.entity.Payment;
import com.example.wagemanager.domain.payment.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findBySalaryId(Long salaryId);
    List<Payment> findByStatus(PaymentStatus status);

    @Query("SELECT p FROM Payment p " +
           "JOIN FETCH p.salary s " +
           "JOIN FETCH s.contract c " +
           "JOIN FETCH c.workplace w " +
           "WHERE w.id = :workplaceId")
    List<Payment> findByWorkplaceId(@Param("workplaceId") Long workplaceId);

    @Query("SELECT p FROM Payment p " +
           "JOIN FETCH p.salary s " +
           "JOIN FETCH s.contract c " +
           "JOIN FETCH c.workplace w " +
           "WHERE w.id = :workplaceId " +
           "AND s.year = :year " +
           "AND s.month = :month")
    List<Payment> findByWorkplaceIdAndYearMonth(
            @Param("workplaceId") Long workplaceId,
            @Param("year") Integer year,
            @Param("month") Integer month
    );

    @Query("SELECT p FROM Payment p " +
           "JOIN FETCH p.salary s " +
           "JOIN FETCH s.contract c " +
           "JOIN FETCH c.workplace w " +
           "WHERE w.id = :workplaceId " +
           "AND s.year = :year " +
           "AND s.month = :month " +
           "AND p.status <> :status")
    List<Payment> findByWorkplaceIdAndYearMonthAndStatusNot(
            @Param("workplaceId") Long workplaceId,
            @Param("year") Integer year,
            @Param("month") Integer month,
            @Param("status") PaymentStatus status
    );

    @Query("SELECT p FROM Payment p " +
           "JOIN FETCH p.salary s " +
           "JOIN FETCH s.contract c " +
           "JOIN FETCH c.workplace w " +
           "WHERE p.status = :status " +
           "AND w.id = :workplaceId")
    List<Payment> findByStatusAndWorkplaceId(
            @Param("status") PaymentStatus status,
            @Param("workplaceId") Long workplaceId
    );

    @Query("SELECT p FROM Payment p " +
           "JOIN FETCH p.salary s " +
           "JOIN FETCH s.contract c " +
           "JOIN FETCH c.worker w " +
           "JOIN FETCH w.user u " +
           "WHERE u.id = :userId " +
           "ORDER BY s.year DESC, s.month DESC")
    List<Payment> findByWorkerUserId(@Param("userId") Long userId);
}
