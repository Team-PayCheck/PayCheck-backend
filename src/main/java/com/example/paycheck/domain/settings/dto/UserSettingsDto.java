package com.example.paycheck.domain.settings.dto;

import com.example.paycheck.domain.settings.entity.UserSettings;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserSettingsDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "UserSettingsResponse")
    public static class Response {
        @Schema(description = "설정 ID")
        private Long id;

        @Schema(description = "사용자 ID")
        private Long userId;

        @Schema(description = "전체 알림 활성화 여부")
        private Boolean notificationEnabled;

        @Schema(description = "푸시 알림 활성화 여부")
        private Boolean pushEnabled;

        @Schema(description = "이메일 알림 활성화 여부")
        private Boolean emailEnabled;

        @Schema(description = "SMS 알림 활성화 여부")
        private Boolean smsEnabled;

        @Schema(description = "일정 변경 알림 활성화 여부")
        private Boolean scheduleChangeAlertEnabled;

        @Schema(description = "급여 알림 활성화 여부")
        private Boolean paymentAlertEnabled;

        @Schema(description = "정정 요청 알림 활성화 여부")
        private Boolean correctionRequestAlertEnabled;

        @Schema(description = "초대 알림 활성화 여부")
        private Boolean invitationAlertEnabled;

        @Schema(description = "퇴사 알림 활성화 여부")
        private Boolean resignationAlertEnabled;

        public static Response from(UserSettings settings) {
            return Response.builder()
                    .id(settings.getId())
                    .userId(settings.getUser().getId())
                    .notificationEnabled(settings.getNotificationEnabled())
                    .pushEnabled(settings.getPushEnabled())
                    .emailEnabled(settings.getEmailEnabled())
                    .smsEnabled(settings.getSmsEnabled())
                    .scheduleChangeAlertEnabled(settings.getScheduleChangeAlertEnabled())
                    .paymentAlertEnabled(settings.getPaymentAlertEnabled())
                    .correctionRequestAlertEnabled(settings.getCorrectionRequestAlertEnabled())
                    .invitationAlertEnabled(settings.getInvitationAlertEnabled())
                    .resignationAlertEnabled(settings.getResignationAlertEnabled())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "UserSettingsUpdateRequest")
    public static class UpdateRequest {
        @Schema(description = "전체 알림 활성화 여부")
        private Boolean notificationEnabled;

        @Schema(description = "푸시 알림 활성화 여부")
        private Boolean pushEnabled;

        @Schema(description = "이메일 알림 활성화 여부")
        private Boolean emailEnabled;

        @Schema(description = "SMS 알림 활성화 여부")
        private Boolean smsEnabled;

        @Schema(description = "일정 변경 알림 활성화 여부")
        private Boolean scheduleChangeAlertEnabled;

        @Schema(description = "급여 알림 활성화 여부")
        private Boolean paymentAlertEnabled;

        @Schema(description = "정정 요청 알림 활성화 여부")
        private Boolean correctionRequestAlertEnabled;

        @Schema(description = "초대 알림 활성화 여부")
        private Boolean invitationAlertEnabled;

        @Schema(description = "퇴사 알림 활성화 여부")
        private Boolean resignationAlertEnabled;
    }
}
