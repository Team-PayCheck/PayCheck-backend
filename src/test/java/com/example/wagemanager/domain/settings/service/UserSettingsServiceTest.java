package com.example.wagemanager.domain.settings.service;

import com.example.wagemanager.common.exception.NotFoundException;
import com.example.wagemanager.domain.settings.dto.NotificationChannels;
import com.example.wagemanager.domain.settings.entity.UserSettings;
import com.example.wagemanager.domain.settings.repository.UserSettingsRepository;
import com.example.wagemanager.domain.user.entity.User;
import com.example.wagemanager.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserSettingsService 테스트")
class UserSettingsServiceTest {

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserSettingsService userSettingsService;

    @Test
    @DisplayName("기본 설정 생성 실패 - 사용자 없음")
    void createDefaultSettings_Fail_UserNotFound() {
        // given
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userSettingsService.createDefaultSettings(userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("기본 설정 생성 성공")
    void createDefaultSettings_Success() {
        // given
        Long userId = 1L;
        User user = mock(User.class);
        UserSettings settings = mock(UserSettings.class);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userSettingsRepository.save(any(UserSettings.class))).thenReturn(settings);

        // when
        UserSettings result = userSettingsService.createDefaultSettings(userId);

        // then
        assertThat(result).isNotNull();
        verify(userRepository).findById(userId);
        verify(userSettingsRepository).save(any(UserSettings.class));
    }

    @Test
    @DisplayName("설정 조회 또는 생성 - 기존 설정 존재")
    void getOrCreateSettings_Existing() {
        // given
        Long userId = 1L;
        UserSettings existingSettings = mock(UserSettings.class);
        when(userSettingsRepository.findByUserId(userId)).thenReturn(Optional.of(existingSettings));

        // when
        UserSettings result = userSettingsService.getOrCreateSettings(userId);

        // then
        assertThat(result).isEqualTo(existingSettings);
        verify(userSettingsRepository).findByUserId(userId);
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("설정 조회 또는 생성 - 새로 생성")
    void getOrCreateSettings_CreateNew() {
        // given
        Long userId = 1L;
        User user = mock(User.class);
        UserSettings newSettings = mock(UserSettings.class);

        when(userSettingsRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userSettingsRepository.save(any(UserSettings.class))).thenReturn(newSettings);

        // when
        UserSettings result = userSettingsService.getOrCreateSettings(userId);

        // then
        assertThat(result).isNotNull();
        verify(userSettingsRepository).findByUserId(userId);
        verify(userRepository).findById(userId);
        verify(userSettingsRepository).save(any(UserSettings.class));
    }

    @Test
    @DisplayName("알림 채널 조회 - 설정 없음")
    void getNotificationChannels_NoSettings() {
        // given
        Long userId = 1L;
        when(userSettingsRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // when
        NotificationChannels result = userSettingsService.getNotificationChannels(userId, "payment");

        // then
        assertThat(result.isShouldSend()).isFalse();
        assertThat(result.getChannels()).isEmpty();
    }

    @Test
    @DisplayName("알림 채널 조회 - 전체 알림 비활성화")
    void getNotificationChannels_NotificationDisabled() {
        // given
        Long userId = 1L;
        UserSettings settings = mock(UserSettings.class);
        when(settings.getNotificationEnabled()).thenReturn(false);
        when(userSettingsRepository.findByUserId(userId)).thenReturn(Optional.of(settings));

        // when
        NotificationChannels result = userSettingsService.getNotificationChannels(userId, "payment");

        // then
        assertThat(result.isShouldSend()).isFalse();
        assertThat(result.getChannels()).isEmpty();
    }

    @Test
    @DisplayName("알림 채널 조회 - 모든 채널 비활성화")
    void getNotificationChannels_AllChannelsDisabled() {
        // given
        Long userId = 1L;
        UserSettings settings = mock(UserSettings.class);
        when(settings.getNotificationEnabled()).thenReturn(true);
        when(settings.getPushEnabled()).thenReturn(false);
        when(settings.getEmailEnabled()).thenReturn(false);
        when(settings.getSmsEnabled()).thenReturn(false);
        when(userSettingsRepository.findByUserId(userId)).thenReturn(Optional.of(settings));

        // when
        NotificationChannels result = userSettingsService.getNotificationChannels(userId, "payment");

        // then
        assertThat(result.isShouldSend()).isFalse();
        assertThat(result.getChannels()).isEmpty();
    }

    @Test
    @DisplayName("알림 채널 조회 - 특정 타입 비활성화 (payment)")
    void getNotificationChannels_TypeDisabled_Payment() {
        // given
        Long userId = 1L;
        UserSettings settings = mock(UserSettings.class);
        when(settings.getNotificationEnabled()).thenReturn(true);
        when(settings.getPushEnabled()).thenReturn(true);
        when(settings.getPaymentAlertEnabled()).thenReturn(false);
        when(userSettingsRepository.findByUserId(userId)).thenReturn(Optional.of(settings));

        // when
        NotificationChannels result = userSettingsService.getNotificationChannels(userId, "payment");

        // then
        assertThat(result.isShouldSend()).isFalse();
    }

    @Test
    @DisplayName("알림 채널 조회 - 특정 타입 비활성화 (schedule_change)")
    void getNotificationChannels_TypeDisabled_ScheduleChange() {
        // given
        Long userId = 1L;
        UserSettings settings = mock(UserSettings.class);
        when(settings.getNotificationEnabled()).thenReturn(true);
        when(settings.getPushEnabled()).thenReturn(true);
        when(settings.getScheduleChangeAlertEnabled()).thenReturn(false);
        when(userSettingsRepository.findByUserId(userId)).thenReturn(Optional.of(settings));

        // when
        NotificationChannels result = userSettingsService.getNotificationChannels(userId, "schedule_change");

        // then
        assertThat(result.isShouldSend()).isFalse();
    }

    @Test
    @DisplayName("알림 채널 조회 - 특정 타입 비활성화 (correction_request)")
    void getNotificationChannels_TypeDisabled_CorrectionRequest() {
        // given
        Long userId = 1L;
        UserSettings settings = mock(UserSettings.class);
        when(settings.getNotificationEnabled()).thenReturn(true);
        when(settings.getPushEnabled()).thenReturn(true);
        when(settings.getCorrectionRequestAlertEnabled()).thenReturn(false);
        when(userSettingsRepository.findByUserId(userId)).thenReturn(Optional.of(settings));

        // when
        NotificationChannels result = userSettingsService.getNotificationChannels(userId, "correction_request");

        // then
        assertThat(result.isShouldSend()).isFalse();
    }

    @Test
    @DisplayName("알림 채널 조회 - Push만 활성화")
    void getNotificationChannels_PushOnly() {
        // given
        Long userId = 1L;
        UserSettings settings = mock(UserSettings.class);
        when(settings.getNotificationEnabled()).thenReturn(true);
        when(settings.getPushEnabled()).thenReturn(true);
        when(settings.getEmailEnabled()).thenReturn(false);
        when(settings.getSmsEnabled()).thenReturn(false);
        when(settings.getPaymentAlertEnabled()).thenReturn(true);
        when(userSettingsRepository.findByUserId(userId)).thenReturn(Optional.of(settings));

        // when
        NotificationChannels result = userSettingsService.getNotificationChannels(userId, "payment");

        // then
        assertThat(result.isShouldSend()).isTrue();
        assertThat(result.getChannels()).hasSize(1);
        assertThat(result.getChannels()).contains("push");
    }

    @Test
    @DisplayName("알림 채널 조회 - 모든 채널 활성화")
    void getNotificationChannels_AllChannelsEnabled() {
        // given
        Long userId = 1L;
        UserSettings settings = mock(UserSettings.class);
        when(settings.getNotificationEnabled()).thenReturn(true);
        when(settings.getPushEnabled()).thenReturn(true);
        when(settings.getEmailEnabled()).thenReturn(true);
        when(settings.getSmsEnabled()).thenReturn(true);
        when(settings.getPaymentAlertEnabled()).thenReturn(true);
        when(userSettingsRepository.findByUserId(userId)).thenReturn(Optional.of(settings));

        // when
        NotificationChannels result = userSettingsService.getNotificationChannels(userId, "payment");

        // then
        assertThat(result.isShouldSend()).isTrue();
        assertThat(result.getChannels()).hasSize(3);
        assertThat(result.getChannels()).containsExactlyInAnyOrder("push", "email", "sms");
    }

    @Test
    @DisplayName("알림 채널 조회 - 알 수 없는 타입 (기본 활성화)")
    void getNotificationChannels_UnknownType() {
        // given
        Long userId = 1L;
        UserSettings settings = mock(UserSettings.class);
        when(settings.getNotificationEnabled()).thenReturn(true);
        when(settings.getPushEnabled()).thenReturn(true);
        when(userSettingsRepository.findByUserId(userId)).thenReturn(Optional.of(settings));

        // when
        NotificationChannels result = userSettingsService.getNotificationChannels(userId, "unknown_type");

        // then
        assertThat(result.isShouldSend()).isTrue();
        assertThat(result.getChannels()).contains("push");
    }
}
