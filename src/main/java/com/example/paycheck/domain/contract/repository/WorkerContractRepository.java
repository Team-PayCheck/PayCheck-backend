package com.example.paycheck.domain.contract.repository;

import com.example.paycheck.domain.contract.entity.WorkerContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkerContractRepository extends JpaRepository<WorkerContract, Long> {
    @Query("SELECT c FROM WorkerContract c " +
            "JOIN FETCH c.worker w " +
            "JOIN FETCH c.workplace wp " +
            "WHERE w.id = :workerId")
    List<WorkerContract> findByWorkerId(@Param("workerId") Long workerId);

    @Query("SELECT c FROM WorkerContract c " +
            "JOIN FETCH c.worker w " +
            "JOIN FETCH c.workplace wp " +
            "WHERE wp.id = :workplaceId")
    List<WorkerContract> findByWorkplaceId(@Param("workplaceId") Long workplaceId);

    @Query("SELECT c FROM WorkerContract c " +
            "JOIN FETCH c.worker w " +
            "JOIN FETCH c.workplace wp " +
            "WHERE wp.id = :workplaceId AND c.isActive = :isActive")
    List<WorkerContract> findByWorkplaceIdAndIsActive(@Param("workplaceId") Long workplaceId, @Param("isActive") Boolean isActive);

    @Query("SELECT c FROM WorkerContract c " +
            "JOIN FETCH c.worker w " +
            "JOIN FETCH c.workplace wp " +
            "WHERE w.id = :workerId AND wp.id = :workplaceId")
    Optional<WorkerContract> findByWorkerIdAndWorkplaceId(@Param("workerId") Long workerId, @Param("workplaceId") Long workplaceId);

    Integer countByWorkplaceIdAndIsActive(Long workplaceId, Boolean isActive);

    /**
     * 내일이 급여 지급일인 활성 계약 조회 (일반 날짜용: paymentDay == tomorrowDay)
     * worker.user까지 JOIN FETCH하여 N+1 방지
     */
    @Query("SELECT c FROM WorkerContract c " +
            "JOIN FETCH c.worker w " +
            "JOIN FETCH w.user u " +
            "JOIN FETCH c.workplace wp " +
            "WHERE c.isActive = true AND c.paymentDay = :tomorrowDay")
    List<WorkerContract> findActiveContractsByExactPaymentDay(@Param("tomorrowDay") int tomorrowDay);

    /**
     * 내일이 급여 지급일인 활성 계약 조회 (월말용: paymentDay >= tomorrowDay)
     * paymentDay=31이지만 2월처럼 해당 일이 없는 경우를 월말로 조정 처리
     */
    @Query("SELECT c FROM WorkerContract c " +
            "JOIN FETCH c.worker w " +
            "JOIN FETCH w.user u " +
            "JOIN FETCH c.workplace wp " +
            "WHERE c.isActive = true AND c.paymentDay >= :tomorrowDay")
    List<WorkerContract> findActiveContractsByPaymentDayOnLastDay(@Param("tomorrowDay") int tomorrowDay);
}
