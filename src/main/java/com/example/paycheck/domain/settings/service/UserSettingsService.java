package com.example.paycheck.domain.settings.service;

import com.example.paycheck.common.exception.ErrorCode;
import com.example.paycheck.common.exception.NotFoundException;
import com.example.paycheck.domain.notification.enums.NotificationType;
import com.example.paycheck.domain.settings.dto.NotificationChannels;
import com.example.paycheck.domain.settings.dto.UserSettingsDto;
import com.example.paycheck.domain.settings.entity.UserSettings;
import com.example.paycheck.domain.settings.repository.UserSettingsRepository;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserSettingsService {

    private final UserSettingsRepository userSettingsRepository;
    private final UserRepository userRepository;

    public UserSettingsDto.Response getUserSettings(Long userId) {
        UserSettings settings = getOrCreateSettings(userId);
        return UserSettingsDto.Response.from(settings);
    }

    @Transactional
    public UserSettingsDto.Response updateUserSettings(Long userId, UserSettingsDto.UpdateRequest request) {
        UserSettings settings = getOrCreateSettings(userId);

        settings.updateSettings(
                request.getNotificationEnabled(),
                request.getPushEnabled(),
                request.getEmailEnabled(),
                request.getSmsEnabled(),
                request.getScheduleChangeAlertEnabled(),
                request.getPaymentAlertEnabled(),
                request.getCorrectionRequestAlertEnabled(),
                request.getInvitationAlertEnabled(),
                request.getResignationAlertEnabled()
        );

        return UserSettingsDto.Response.from(settings);
    }

    @Transactional
    public UserSettings getOrCreateSettings(Long userId) {
        return userSettingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));
    }

    @Transactional
    public UserSettings createDefaultSettings(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        UserSettings settings = UserSettings.builder()
                .user(user)
                .build();

        return userSettingsRepository.save(settings);
    }

    /**
     * 알림 전송 여부와 사용 가능한 채널 목록을 반환합니다.
     *
     * @param userId 사용자 ID
     * @param notificationType 알림 타입 (schedule_change, payment, correction_request)
     * @return 전송 여부와 활성화된 채널 목록 (push, email, sms)
     */
    public NotificationChannels getNotificationChannels(Long userId, String notificationType) {
        UserSettings settings = userSettingsRepository.findByUserId(userId)
                .orElse(null);

        // 설정이 없거나 전체 알림이 비활성화된 경우
        if (settings == null || !settings.getNotificationEnabled()) {
            return NotificationChannels.disabled();
        }

        // 활성화된 채널 목록 수집
        List<String> channels = new ArrayList<>();
        if (settings.getPushEnabled()) {
            channels.add("push");
        }
        if (settings.getEmailEnabled()) {
            channels.add("email");
        }
        if (settings.getSmsEnabled()) {
            channels.add("sms");
        }

        // 활성화된 채널이 없으면 전송 안 함
        if (channels.isEmpty()) {
            return NotificationChannels.disabled();
        }

        // 알림 타입별 활성화 여부 확인
        boolean typeEnabled = switch (notificationType.toLowerCase()) {
            case "schedule_change" -> settings.getScheduleChangeAlertEnabled();
            case "payment" -> settings.getPaymentAlertEnabled();
            case "correction_request" -> settings.getCorrectionRequestAlertEnabled();
            default -> true; // 기본적으로 알림 전송
        };

        if (!typeEnabled) {
            return NotificationChannels.disabled();
        }

        return NotificationChannels.of(true, channels);
    }

    /**
     * NotificationType enum을 직접 받아 알림 전송 여부와 채널 목록을 반환합니다.
     *
     * @param userId 사용자 ID
     * @param notificationType 알림 타입 enum
     * @return 전송 여부와 활성화된 채널 목록 (push, email, sms)
     */
    public NotificationChannels getNotificationChannels(Long userId, NotificationType notificationType) {
        UserSettings settings = userSettingsRepository.findByUserId(userId)
                .orElse(null);

        // 설정이 없거나 전체 알림이 비활성화된 경우
        if (settings == null || !settings.getNotificationEnabled()) {
            return NotificationChannels.disabled();
        }

        // 활성화된 채널 목록 수집
        List<String> channels = new ArrayList<>();
        if (settings.getPushEnabled()) {
            channels.add("push");
        }
        if (settings.getEmailEnabled()) {
            channels.add("email");
        }
        if (settings.getSmsEnabled()) {
            channels.add("sms");
        }

        // 활성화된 채널이 없으면 전송 안 함
        if (channels.isEmpty()) {
            return NotificationChannels.disabled();
        }

        // 알림 타입별 활성화 여부 확인
        boolean typeEnabled = isNotificationTypeEnabled(settings, notificationType);

        if (!typeEnabled) {
            return NotificationChannels.disabled();
        }

        return NotificationChannels.of(true, channels);
    }

    private boolean isNotificationTypeEnabled(UserSettings settings, NotificationType type) {
        return switch (type) {
            case SCHEDULE_CHANGE, SCHEDULE_CREATED, SCHEDULE_APPROVAL_REQUEST,
                 SCHEDULE_APPROVED, SCHEDULE_REJECTED, SCHEDULE_DELETED
                -> settings.getScheduleChangeAlertEnabled();
            case CORRECTION_RESPONSE, UNREAD_CORRECTION_REQUEST
                -> settings.getCorrectionRequestAlertEnabled();
            case PAYMENT_DUE, PAYMENT_SUCCESS, PAYMENT_FAILED
                -> settings.getPaymentAlertEnabled();
            case INVITATION -> settings.getInvitationAlertEnabled();
            case RESIGNATION -> settings.getResignationAlertEnabled();
        };
    }
}
