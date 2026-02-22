package com.example.paycheck.domain.correction.entity;

import com.example.paycheck.common.BaseEntity;
import com.example.paycheck.domain.contract.entity.WorkerContract;
import com.example.paycheck.domain.correction.enums.CorrectionStatus;
import com.example.paycheck.domain.correction.enums.RequestType;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.workrecord.entity.WorkRecord;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "correction_request")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CorrectionRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private RequestType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_record_id", nullable = true)
    private WorkRecord workRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = true)
    private WorkerContract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    // 원본 근무 시간 (정정요청 생성 시점의 값 저장) - CREATE 타입의 경우 null
    @Column(name = "original_work_date")
    private LocalDate originalWorkDate;

    @Column(name = "original_start_time")
    private LocalTime originalStartTime;

    @Column(name = "original_end_time")
    private LocalTime originalEndTime;

    // 요청 근무 시간
    @Column(name = "requested_work_date", nullable = false)
    private LocalDate requestedWorkDate;

    @Column(name = "requested_start_time", nullable = false)
    private LocalTime requestedStartTime;

    @Column(name = "requested_end_time", nullable = false)
    private LocalTime requestedEndTime;

    @Column(name = "requested_break_minutes")
    private Integer requestedBreakMinutes;

    @Column(name = "requested_memo", columnDefinition = "TEXT")
    private String requestedMemo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CorrectionStatus status = CorrectionStatus.PENDING;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    public void approve() {
        this.status = CorrectionStatus.APPROVED;
        this.reviewedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = CorrectionStatus.REJECTED;
        this.reviewedAt = LocalDateTime.now();
    }

    // Helper 메서드
    public boolean isCreateType() {
        return this.type == RequestType.CREATE;
    }

    public boolean isUpdateType() {
        return this.type == RequestType.UPDATE;
    }

    public boolean isDeleteType() {
        return this.type == RequestType.DELETE;
    }
}
