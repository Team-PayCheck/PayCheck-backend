package com.example.paycheck.domain.correction.repository;

import com.example.paycheck.domain.correction.entity.CorrectionRequest;
import com.example.paycheck.domain.correction.enums.CorrectionStatus;
import com.example.paycheck.domain.correction.enums.RequestType;
import com.example.paycheck.domain.workrecord.enums.WorkRecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CorrectionRequestRepository extends JpaRepository<CorrectionRequest, Long> {

        // 근로자 본인의 정정요청 목록 조회
        @Query("SELECT cr FROM CorrectionRequest cr " +
                        "LEFT JOIN FETCH cr.workRecord wr " +
                        "LEFT JOIN FETCH wr.contract wrc " +
                        "LEFT JOIN FETCH wrc.workplace wrw " +
                        "LEFT JOIN FETCH cr.contract cc " +
                        "LEFT JOIN FETCH cc.workplace cw " +
                        "WHERE cr.requester.id = :requesterId " +
                        "ORDER BY cr.createdAt DESC")
        List<CorrectionRequest> findByRequesterId(@Param("requesterId") Long requesterId);

        // 근로자 본인의 정정요청 상태별 조회
        @Query("SELECT cr FROM CorrectionRequest cr " +
                        "LEFT JOIN FETCH cr.workRecord wr " +
                        "LEFT JOIN FETCH wr.contract wrc " +
                        "LEFT JOIN FETCH wrc.workplace wrw " +
                        "LEFT JOIN FETCH cr.contract cc " +
                        "LEFT JOIN FETCH cc.workplace cw " +
                        "WHERE cr.requester.id = :requesterId " +
                        "AND cr.status = :status " +
                        "ORDER BY cr.createdAt DESC")
        List<CorrectionRequest> findByRequesterIdAndStatus(
                        @Param("requesterId") Long requesterId,
                        @Param("status") CorrectionStatus status);

        // 사업장별 정정요청 목록 조회 (고용주용)
        @Query("SELECT cr FROM CorrectionRequest cr " +
                        "LEFT JOIN FETCH cr.workRecord wr " +
                        "LEFT JOIN FETCH wr.contract wrc " +
                        "LEFT JOIN FETCH wrc.workplace wrw " +
                        "LEFT JOIN FETCH cr.contract cc " +
                        "LEFT JOIN FETCH cc.workplace cw " +
                        "JOIN FETCH cr.requester r " +
                        "WHERE (wrw.id = :workplaceId OR cw.id = :workplaceId) " +
                        "ORDER BY cr.createdAt DESC")
        List<CorrectionRequest> findByWorkplaceId(@Param("workplaceId") Long workplaceId);

        // 사업장별 + 상태별 정정요청 목록 조회 (고용주용)
        @Query("SELECT cr FROM CorrectionRequest cr " +
                        "LEFT JOIN FETCH cr.workRecord wr " +
                        "LEFT JOIN FETCH wr.contract wrc " +
                        "LEFT JOIN FETCH wrc.workplace wrw " +
                        "LEFT JOIN FETCH cr.contract cc " +
                        "LEFT JOIN FETCH cc.workplace cw " +
                        "JOIN FETCH cr.requester r " +
                        "WHERE (wrw.id = :workplaceId OR cw.id = :workplaceId) " +
                        "AND cr.status = :status " +
                        "ORDER BY cr.createdAt DESC")
        List<CorrectionRequest> findByWorkplaceIdAndStatus(
                        @Param("workplaceId") Long workplaceId,
                        @Param("status") CorrectionStatus status);

        // 사업장별 + 상태별 + 타입별 정정요청 목록 조회 (고용주용)
        @Query("SELECT cr FROM CorrectionRequest cr " +
                        "LEFT JOIN FETCH cr.workRecord wr " +
                        "LEFT JOIN FETCH wr.contract wrc " +
                        "LEFT JOIN FETCH wrc.workplace wrw " +
                        "LEFT JOIN FETCH cr.contract cc " +
                        "LEFT JOIN FETCH cc.workplace cw " +
                        "JOIN FETCH cr.requester r " +
                        "WHERE (wrw.id = :workplaceId OR cw.id = :workplaceId) " +
                        "AND cr.status = :status " +
                        "AND cr.type = :type " +
                        "ORDER BY cr.createdAt DESC")
        List<CorrectionRequest> findByWorkplaceIdAndStatusAndType(
                        @Param("workplaceId") Long workplaceId,
                        @Param("status") CorrectionStatus status,
                        @Param("type") RequestType type);

        // 정정요청 상세 조회 (fetch join)
        @Query("SELECT cr FROM CorrectionRequest cr " +
                        "LEFT JOIN FETCH cr.workRecord wr " +
                        "LEFT JOIN FETCH wr.contract wrc " +
                        "LEFT JOIN FETCH wrc.workplace wrw " +
                        "LEFT JOIN FETCH cr.contract cc " +
                        "LEFT JOIN FETCH cc.workplace cw " +
                        "JOIN FETCH cr.requester r " +
                        "WHERE cr.id = :id")
        Optional<CorrectionRequest> findByIdWithDetails(@Param("id") Long id);

        // 특정 근무기록에 대한 대기중인 정정요청 존재 여부 확인
        @Query("SELECT COUNT(cr) > 0 FROM CorrectionRequest cr " +
                        "WHERE cr.workRecord.id = :workRecordId " +
                        "AND cr.status = :status")
        boolean existsByWorkRecordIdAndStatus(
                        @Param("workRecordId") Long workRecordId,
                        @Param("status") CorrectionStatus status);

        // CREATE 타입의 대기중인 정정요청 존재 여부 확인 - 동일한 시간대에 요청이 있는지 확인
        @Query("SELECT COUNT(cr) > 0 FROM CorrectionRequest cr " +
                        "WHERE cr.contract.id = :contractId " +
                        "AND cr.type = :type " +
                        "AND cr.requestedWorkDate = :workDate " +
                        "AND cr.status = :status " +
                        "AND (cr.requestedStartTime < :endTime AND cr.requestedEndTime > :startTime)")
        boolean existsPendingCreateRequestWithTimeOverlap(
                        @Param("contractId") Long contractId,
                        @Param("workDate") LocalDate workDate,
                        @Param("startTime") LocalTime startTime,
                        @Param("endTime") LocalTime endTime,
                        @Param("type") RequestType type,
                        @Param("status") CorrectionStatus status);

        // 계약 정보 변경 시 미래 WorkRecord를 참조하는 CorrectionRequest 삭제
        @Modifying
        @Query("DELETE FROM CorrectionRequest cr " +
                        "WHERE cr.workRecord.id IN " +
                        "(SELECT wr.id FROM WorkRecord wr WHERE wr.contract.id = :contractId " +
                        "AND wr.workDate > :date AND wr.status = :status)")
        void deleteByWorkRecordContractAndDateAfterAndStatus(
                        @Param("contractId") Long contractId,
                        @Param("date") LocalDate date,
                        @Param("status") WorkRecordStatus status);
}
