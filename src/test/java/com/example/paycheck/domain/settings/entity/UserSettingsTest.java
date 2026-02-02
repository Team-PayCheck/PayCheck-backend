package com.example.paycheck.domain.settings.entity;

import com.example.paycheck.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("UserSettings 엔티티 테스트")
class UserSettingsTest {

    private UserSettings userSettings;
    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = mock(User.class);
        userSettings = UserSettings.builder()
                .id(1L)
                .user(mockUser)
                .notificationEnabled(true)
                .pushEnabled(true)
                .emailEnabled(false)
                .smsEnabled(false)
                .scheduleChangeAlertEnabled(true)
                .paymentAlertEnabled(true)
                .correctionRequestAlertEnabled(true)
                .build();
    }

    @Test
    @DisplayName("설정 업데이트 - 모든 필드")
    void updateSettings_AllFields() {
        // when
        userSettings.updateSettings(
                false,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                false
        );

        // then
        assertThat(userSettings.getNotificationEnabled()).isFalse();
        assertThat(userSettings.getPushEnabled()).isFalse();
        assertThat(userSettings.getEmailEnabled()).isTrue();
        assertThat(userSettings.getSmsEnabled()).isTrue();
        assertThat(userSettings.getScheduleChangeAlertEnabled()).isFalse();
        assertThat(userSettings.getPaymentAlertEnabled()).isFalse();
        assertThat(userSettings.getCorrectionRequestAlertEnabled()).isFalse();
        assertThat(userSettings.getInvitationAlertEnabled()).isFalse();
        assertThat(userSettings.getResignationAlertEnabled()).isFalse();
    }

    @Test
    @DisplayName("설정 업데이트 - 일부 필드만")
    void updateSettings_PartialFields() {
        // when
        userSettings.updateSettings(
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        // then
        assertThat(userSettings.getNotificationEnabled()).isFalse();
        assertThat(userSettings.getPushEnabled()).isTrue(); // unchanged
        assertThat(userSettings.getEmailEnabled()).isFalse(); // unchanged
    }

    @Test
    @DisplayName("설정 업데이트 - null 값은 변경되지 않음")
    void updateSettings_NullValues() {
        // given
        Boolean originalNotificationEnabled = userSettings.getNotificationEnabled();
        Boolean originalPushEnabled = userSettings.getPushEnabled();

        // when
        userSettings.updateSettings(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        // then
        assertThat(userSettings.getNotificationEnabled()).isEqualTo(originalNotificationEnabled);
        assertThat(userSettings.getPushEnabled()).isEqualTo(originalPushEnabled);
    }

    @Test
    @DisplayName("기본 설정 확인")
    void defaultSettings() {
        // given
        UserSettings defaultSettings = UserSettings.builder()
                .user(mockUser)
                .build();

        // then
        assertThat(defaultSettings.getNotificationEnabled()).isTrue();
        assertThat(defaultSettings.getPushEnabled()).isTrue();
        assertThat(defaultSettings.getEmailEnabled()).isFalse();
        assertThat(defaultSettings.getSmsEnabled()).isFalse();
        assertThat(defaultSettings.getScheduleChangeAlertEnabled()).isTrue();
        assertThat(defaultSettings.getPaymentAlertEnabled()).isTrue();
        assertThat(defaultSettings.getCorrectionRequestAlertEnabled()).isTrue();
        assertThat(defaultSettings.getInvitationAlertEnabled()).isTrue();
        assertThat(defaultSettings.getResignationAlertEnabled()).isTrue();
    }
}
