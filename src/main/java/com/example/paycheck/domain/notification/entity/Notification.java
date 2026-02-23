package com.example.paycheck.domain.notification.entity;

import com.example.paycheck.common.BaseEntity;
import com.example.paycheck.domain.notification.enums.NotificationActionType;
import com.example.paycheck.domain.notification.enums.NotificationType;
import com.example.paycheck.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification",
        indexes = {
                @Index(name = "idx_user_read_created", columnList = "user_id,is_read,created_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    @Column(name = "title", nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "action_type", nullable = false, length = 50)
    @Builder.Default
    private NotificationActionType actionType = NotificationActionType.NONE;

    /**
     * 액션 수행에 필요한 데이터 (JSON 형식)
     * 예: {"correctionRequestId": 123, "workRecordId": 456}
     */
    @Column(name = "action_data", columnDefinition = "TEXT")
    private String actionData;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
}
