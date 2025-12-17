package com.example.wagemanager.domain.workrecord.deletion.entity;

import com.example.wagemanager.common.BaseEntity;
import com.example.wagemanager.domain.user.entity.User;
import com.example.wagemanager.domain.workrecord.deletion.enums.WorkRecordDeletionRequestStatus;
import com.example.wagemanager.domain.workrecord.entity.WorkRecord;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@Table(name = "work_record_deletion_request",
        indexes = {
                @Index(name = "idx_work_record_status", columnList = "work_record_id,status")
        })
public class WorkRecordDeletionRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_record_id")
    private WorkRecord workRecord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id")
    private User requester;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private WorkRecordDeletionRequestStatus status = WorkRecordDeletionRequestStatus.PENDING;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    public void approve() {
        this.status = WorkRecordDeletionRequestStatus.APPROVED;
        this.reviewedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = WorkRecordDeletionRequestStatus.REJECTED;
        this.reviewedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = WorkRecordDeletionRequestStatus.CANCELLED;
        this.reviewedAt = LocalDateTime.now();
    }
}
