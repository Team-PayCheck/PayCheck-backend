package com.example.paycheck.domain.payment.repository;

import com.example.paycheck.domain.payment.entity.Payment;
import com.example.paycheck.domain.payment.enums.PaymentStatus;
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

    /**
     * Payment 조회 시 연관된 모든 엔티티를 함께 조회 (N+1 문제 방지)
     * - Salary, Contract, Worker, User, Workplace를 JOIN FETCH
     */
    @Query("SELECT p FROM Payment p " +
           "JOIN FETCH p.salary s " +
           "JOIN FETCH s.contract c " +
           "JOIN FETCH c.worker w " +
           "JOIN FETCH w.user u " +
           "JOIN FETCH c.workplace wp " +
           "WHERE p.id = :paymentId")
    Optional<Payment> findByIdWithAssociations(@Param("paymentId") Long paymentId);

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

    @Query("SELECT p FROM Payment p " +
           "JOIN FETCH p.salary s " +
           "JOIN FETCH s.contract c " +
           "JOIN FETCH c.worker w " +
           "JOIN FETCH w.user u " +
           "WHERE u.id = :userId " +
           "AND s.year = :year " +
           "AND s.month = :month " +
           "ORDER BY s.year DESC, s.month DESC")
    List<Payment> findByWorkerUserIdAndYearMonth(
            @Param("userId") Long userId,
            @Param("year") Integer year,
            @Param("month") Integer month);
}
