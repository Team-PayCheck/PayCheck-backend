package com.example.paycheck.domain.workrecord.repository;

import com.example.paycheck.domain.workrecord.entity.WorkRecord;
import com.example.paycheck.domain.workrecord.enums.WorkRecordStatus;
import com.example.paycheck.domain.contract.entity.WorkerContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface WorkRecordRepository extends JpaRepository<WorkRecord, Long> {

        @Query("SELECT wr FROM WorkRecord wr " +
                        "JOIN FETCH wr.contract c " +
                        "JOIN FETCH c.worker w " +
                        "WHERE c.id = :contractId")
        List<WorkRecord> findByContractId(@Param("contractId") Long contractId);

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
                        @Param("endDate") LocalDate endDate);

        @Query("SELECT wr FROM WorkRecord wr " +
                        "JOIN FETCH wr.contract c " +
                        "JOIN FETCH c.worker wk " +
                        "WHERE wk.id = :workerId " +
                        "AND wr.workDate BETWEEN :startDate AND :endDate " +
                        "ORDER BY wr.workDate ASC")
        List<WorkRecord> findByWorkerAndDateRange(
                        @Param("workerId") Long workerId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("SELECT wr FROM WorkRecord wr " +
                        "JOIN FETCH wr.contract c " +
                        "WHERE c.id = :contractId " +
                        "AND wr.workDate BETWEEN :startDate AND :endDate " +
                        "AND wr.status IN :statuses")
        List<WorkRecord> findByContractAndDateRangeAndStatus(
                        @Param("contractId") Long contractId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("statuses") List<WorkRecordStatus> statuses);

        @Query("SELECT wr FROM WorkRecord wr " +
                        "JOIN FETCH wr.contract c " +
                        "WHERE c.id = :contractId " +
                        "AND wr.workDate BETWEEN :startDate AND :endDate " +
                        "ORDER BY wr.workDate ASC")
        List<WorkRecord> findByContractAndDateRange(
                        @Param("contractId") Long contractId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("SELECT DISTINCT c FROM WorkerContract c " +
                        "JOIN FETCH c.worker w " +
                        "JOIN FETCH c.workplace " +
                        "WHERE c.workplace.id = :workplaceId " +
                        "AND c.isActive = true")
        List<WorkerContract> findContractsByWorkplaceId(
                        @Param("workplaceId") Long workplaceId);

        boolean existsByContractAndWorkDate(WorkerContract contract, LocalDate workDate);

        @Query("SELECT c FROM WorkerContract c " +
                        "JOIN FETCH c.worker w " +
                        "JOIN FETCH c.workplace " +
                        "WHERE c.isActive = true")
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
                        @Param("status") WorkRecordStatus status);

        // 계약 정보 변경 시 미래 WorkRecord 삭제
        @Modifying
        @Query("DELETE FROM WorkRecord wr " +
                        "WHERE wr.contract.id = :contractId " +
                        "AND wr.workDate > :date " +
                        "AND wr.status = :status")
        void deleteByContractIdAndWorkDateAfterAndStatus(
                        @Param("contractId") Long contractId,
                        @Param("date") LocalDate date,
                        @Param("status") WorkRecordStatus status);

        // 동일한 시간대에 유효한(삭제되지 않은) 근무 기록 존재 여부 확인
        @Query("SELECT COUNT(wr) > 0 FROM WorkRecord wr " +
                        "WHERE wr.contract.id = :contractId " +
                        "AND wr.workDate = :workDate " +
                        "AND wr.status <> :deletedStatus " +
                        "AND (wr.startTime < :endTime AND wr.endTime > :startTime)")
        boolean existsOverlappingWorkRecord(
                        @Param("contractId") Long contractId,
                        @Param("workDate") LocalDate workDate,
                        @Param("startTime") LocalTime startTime,
                        @Param("endTime") LocalTime endTime,
                        @Param("deletedStatus") WorkRecordStatus deletedStatus);

        // 특정 계약의 여러 날짜에 대한 기존 WorkRecord 날짜 일괄 조회 (배치 중복 체크용)
        @Query("SELECT wr.workDate FROM WorkRecord wr " +
                        "WHERE wr.contract.id = :contractId " +
                        "AND wr.workDate IN :workDates " +
                        "AND wr.status <> :deletedStatus")
        List<LocalDate> findExistingWorkDatesByContractAndWorkDates(
                        @Param("contractId") Long contractId,
                        @Param("workDates") List<LocalDate> workDates,
                        @Param("deletedStatus") WorkRecordStatus deletedStatus);

        // 현재 시점 기준으로 종료된 SCHEDULED 근무 기록 조회 (자동 완료 배치용)
        @Query("SELECT wr FROM WorkRecord wr " +
                        "JOIN FETCH wr.contract c " +
                        "JOIN FETCH c.workplace " +
                        "WHERE wr.status = :status " +
                        "AND ( " +
                        "   (wr.endTime > wr.startTime " +
                        "       AND (wr.workDate < :currentDate " +
                        "            OR (wr.workDate = :currentDate AND wr.endTime <= :currentTime))) " +
                        "   OR " +
                        "   (wr.endTime <= wr.startTime " +
                        "       AND (wr.workDate < :previousDate " +
                        "            OR (wr.workDate = :previousDate AND wr.endTime <= :currentTime))) " +
                        ") " +
                        "ORDER BY wr.workDate ASC, wr.endTime ASC")
        List<WorkRecord> findPastScheduledWorkRecords(
                        @Param("status") WorkRecordStatus status,
                        @Param("currentDate") LocalDate currentDate,
                        @Param("currentTime") LocalTime currentTime,
                        @Param("previousDate") LocalDate previousDate);
}
