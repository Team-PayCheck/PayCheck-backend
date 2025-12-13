package com.example.wagemanager.domain.workrecord.repository;

import com.example.wagemanager.domain.workrecord.entity.WorkRecord;
import com.example.wagemanager.domain.workrecord.enums.WorkRecordStatus;
import com.example.wagemanager.domain.contract.entity.WorkerContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WorkRecordRepository extends JpaRepository<WorkRecord, Long> {

    List<WorkRecord> findByContractId(Long contractId);

    @Query("SELECT wr FROM WorkRecord wr " +
            "JOIN FETCH wr.contract c " +
            "JOIN FETCH c.workplace w " +
            "JOIN FETCH c.worker wk " +
            "WHERE w.id = :workplaceId " +
            "AND wr.workDate BETWEEN :startDate AND :endDate " +
            "ORDER BY wr.workDate ASC")
    List<WorkRecord> findByWorkplaceAndDateRange(
            @Param("workplaceId") Long workplaceId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT wr FROM WorkRecord wr " +
            "JOIN FETCH wr.contract c " +
            "JOIN FETCH c.worker wk " +
            "WHERE wk.id = :workerId " +
            "AND wr.workDate BETWEEN :startDate AND :endDate " +
            "ORDER BY wr.workDate ASC")
    List<WorkRecord> findByWorkerAndDateRange(
            @Param("workerId") Long workerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT wr FROM WorkRecord wr " +
            "WHERE wr.contract.id = :contractId " +
            "AND wr.workDate BETWEEN :startDate AND :endDate " +
            "AND wr.status IN :statuses")
    List<WorkRecord> findByContractAndDateRangeAndStatus(
            @Param("contractId") Long contractId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") List<WorkRecordStatus> statuses
    );

    @Query("SELECT wr FROM WorkRecord wr " +
            "WHERE wr.contract.id = :contractId " +
            "AND wr.workDate BETWEEN :startDate AND :endDate " +
            "ORDER BY wr.workDate ASC")
    List<WorkRecord> findByContractAndDateRange(
            @Param("contractId") Long contractId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT DISTINCT c FROM WorkerContract c " +
            "WHERE c.workplace.id = :workplaceId " +
            "AND c.isActive = true")
    List<WorkerContract> findContractsByWorkplaceId(
            @Param("workplaceId") Long workplaceId
    );

    boolean existsByContractAndWorkDate(WorkerContract contract, LocalDate workDate);

    @Query("SELECT c FROM WorkerContract c WHERE c.isActive = true")
    List<WorkerContract> findAllActiveContracts();

    // 사업장별 승인 대기중인 근무 기록 조회
    @Query("SELECT wr FROM WorkRecord wr " +
            "JOIN FETCH wr.contract c " +
            "JOIN FETCH c.workplace " +
            "JOIN FETCH c.worker w " +
            "JOIN FETCH w.user " +
            "WHERE c.workplace.id = :workplaceId " +
            "AND wr.status = :status " +
            "ORDER BY wr.workDate ASC")
    List<WorkRecord> findByWorkplaceAndStatus(
            @Param("workplaceId") Long workplaceId,
            @Param("status") WorkRecordStatus status
    );
}
