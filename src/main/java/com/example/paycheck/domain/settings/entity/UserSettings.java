package com.example.paycheck.domain.settings.entity;

import com.example.paycheck.common.BaseEntity;
import com.example.paycheck.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserSettings extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "notification_enabled", nullable = false)
    @Builder.Default
    private Boolean notificationEnabled = true;

    @Column(name = "push_enabled", nullable = false)
    @Builder.Default
    private Boolean pushEnabled = true;

    @Column(name = "email_enabled", nullable = false)
    @Builder.Default
    private Boolean emailEnabled = false;

    @Column(name = "sms_enabled", nullable = false)
    @Builder.Default
    private Boolean smsEnabled = false;

    @Column(name = "schedule_change_alert_enabled", nullable = false)
    @Builder.Default
    private Boolean scheduleChangeAlertEnabled = true;

    @Column(name = "payment_alert_enabled", nullable = false)
    @Builder.Default
    private Boolean paymentAlertEnabled = true;

    @Column(name = "correction_request_alert_enabled", nullable = false)
    @Builder.Default
    private Boolean correctionRequestAlertEnabled = true;

    @Column(name = "invitation_alert_enabled", nullable = false)
    @Builder.Default
    private Boolean invitationAlertEnabled = true;

    @Column(name = "resignation_alert_enabled", nullable = false)
    @Builder.Default
    private Boolean resignationAlertEnabled = true;

    public void updateSettings(
            Boolean notificationEnabled,
            Boolean pushEnabled,
            Boolean emailEnabled,
            Boolean smsEnabled,
            Boolean scheduleChangeAlertEnabled,
            Boolean paymentAlertEnabled,
            Boolean correctionRequestAlertEnabled,
            Boolean invitationAlertEnabled,
            Boolean resignationAlertEnabled
    ) {
        if (notificationEnabled != null) this.notificationEnabled = notificationEnabled;
        if (pushEnabled != null) this.pushEnabled = pushEnabled;
        if (emailEnabled != null) this.emailEnabled = emailEnabled;
        if (smsEnabled != null) this.smsEnabled = smsEnabled;
        if (scheduleChangeAlertEnabled != null) this.scheduleChangeAlertEnabled = scheduleChangeAlertEnabled;
        if (paymentAlertEnabled != null) this.paymentAlertEnabled = paymentAlertEnabled;
        if (correctionRequestAlertEnabled != null) this.correctionRequestAlertEnabled = correctionRequestAlertEnabled;
        if (invitationAlertEnabled != null) this.invitationAlertEnabled = invitationAlertEnabled;
        if (resignationAlertEnabled != null) this.resignationAlertEnabled = resignationAlertEnabled;
    }
}
