package com.example.paycheck.domain.notification.event;

import com.example.paycheck.domain.notification.entity.Notification;
import com.example.paycheck.domain.notification.enums.NotificationActionType;
import com.example.paycheck.domain.notification.enums.NotificationType;
import com.example.paycheck.domain.notification.repository.NotificationRepository;
import com.example.paycheck.domain.notification.service.SseEmitterService;
import com.example.paycheck.domain.settings.dto.NotificationChannels;
import com.example.paycheck.domain.settings.service.UserSettingsService;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.user.enums.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationEventListener 테스트")
class NotificationEventListenerTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SseEmitterService sseEmitterService;

    @Mock
    private UserSettingsService userSettingsService;

    @InjectMocks
    private NotificationEventListener notificationEventListener;

    private User testUser;
    private NotificationEvent testEvent;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .kakaoId("test_kakao")
                .name("테스트 사용자")
                .userType(UserType.WORKER)
                .build();

        testEvent = NotificationEvent.builder()
                .user(testUser)
                .type(NotificationType.INVITATION)
                .title("테스트 알림")
                .actionType(NotificationActionType.VIEW_WORKPLACE_INVITATION)
                .actionData("{\"workplaceId\": 1}")
                .build();
    }

    @Test
    @DisplayName("알림 이벤트 처리 - 설정 활성화 시 저장 및 SSE 전송")
    void handleNotificationEvent_ShouldSaveAndSend_WhenSettingsEnabled() {
        // given
        NotificationChannels channels = NotificationChannels.of(true, List.of("push"));
        when(userSettingsService.getNotificationChannels(eq(1L), eq(NotificationType.INVITATION)))
                .thenReturn(channels);

        Notification savedNotification = Notification.builder()
                .id(1L)
                .user(testUser)
                .type(NotificationType.INVITATION)
                .title("테스트 알림")
                .actionType(NotificationActionType.VIEW_WORKPLACE_INVITATION)
                .build();
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        // when
        notificationEventListener.handleNotificationEvent(testEvent);

        // then
        verify(userSettingsService).getNotificationChannels(eq(1L), eq(NotificationType.INVITATION));
        verify(notificationRepository).save(any(Notification.class));
        verify(sseEmitterService).sendNotification(eq(1L), any(Notification.class));
    }

    @Test
    @DisplayName("알림 이벤트 처리 - 전체 알림 비활성화 시 저장/전송 스킵")
    void handleNotificationEvent_ShouldSkip_WhenGlobalNotificationDisabled() {
        // given
        NotificationChannels channels = NotificationChannels.disabled();
        when(userSettingsService.getNotificationChannels(eq(1L), eq(NotificationType.INVITATION)))
                .thenReturn(channels);

        // when
        notificationEventListener.handleNotificationEvent(testEvent);

        // then
        verify(userSettingsService).getNotificationChannels(eq(1L), eq(NotificationType.INVITATION));
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(sseEmitterService, never()).sendNotification(anyLong(), any(Notification.class));
    }

    @Test
    @DisplayName("알림 이벤트 처리 - 특정 타입 비활성화 시 저장/전송 스킵")
    void handleNotificationEvent_ShouldSkip_WhenTypeDisabled() {
        // given
        NotificationChannels channels = NotificationChannels.disabled();
        when(userSettingsService.getNotificationChannels(eq(1L), eq(NotificationType.INVITATION)))
                .thenReturn(channels);

        // when
        notificationEventListener.handleNotificationEvent(testEvent);

        // then
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(sseEmitterService, never()).sendNotification(anyLong(), any(Notification.class));
    }

    @Test
    @DisplayName("알림 이벤트 처리 - Push 비활성화 시 SSE만 스킵")
    void handleNotificationEvent_ShouldSkipSse_WhenPushDisabled() {
        // given
        NotificationChannels channels = NotificationChannels.of(true, List.of("email", "sms"));
        when(userSettingsService.getNotificationChannels(eq(1L), eq(NotificationType.INVITATION)))
                .thenReturn(channels);

        Notification savedNotification = Notification.builder()
                .id(1L)
                .user(testUser)
                .type(NotificationType.INVITATION)
                .title("테스트 알림")
                .actionType(NotificationActionType.VIEW_WORKPLACE_INVITATION)
                .build();
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        // when
        notificationEventListener.handleNotificationEvent(testEvent);

        // then
        verify(notificationRepository).save(any(Notification.class));
        verify(sseEmitterService, never()).sendNotification(anyLong(), any(Notification.class));
    }

    @Test
    @DisplayName("알림 이벤트 처리 - 다양한 알림 타입 (SCHEDULE_CREATED)")
    void handleNotificationEvent_ScheduleCreated() {
        // given
        NotificationEvent scheduleEvent = NotificationEvent.builder()
                .user(testUser)
                .type(NotificationType.SCHEDULE_CREATED)
                .title("근무 일정이 생성되었습니다")
                .actionType(NotificationActionType.VIEW_WORK_RECORD)
                .build();

        NotificationChannels channels = NotificationChannels.of(true, List.of("push"));
        when(userSettingsService.getNotificationChannels(eq(1L), eq(NotificationType.SCHEDULE_CREATED)))
                .thenReturn(channels);

        Notification savedNotification = Notification.builder()
                .id(1L)
                .user(testUser)
                .type(NotificationType.SCHEDULE_CREATED)
                .title("근무 일정이 생성되었습니다")
                .actionType(NotificationActionType.VIEW_WORK_RECORD)
                .build();
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        // when
        notificationEventListener.handleNotificationEvent(scheduleEvent);

        // then
        verify(userSettingsService).getNotificationChannels(eq(1L), eq(NotificationType.SCHEDULE_CREATED));
        verify(notificationRepository).save(any(Notification.class));
        verify(sseEmitterService).sendNotification(eq(1L), any(Notification.class));
    }

    @Test
    @DisplayName("알림 이벤트 처리 - 다양한 알림 타입 (RESIGNATION)")
    void handleNotificationEvent_Resignation() {
        // given
        NotificationEvent resignationEvent = NotificationEvent.builder()
                .user(testUser)
                .type(NotificationType.RESIGNATION)
                .title("퇴사 처리되었습니다")
                .actionType(NotificationActionType.NONE)
                .build();

        NotificationChannels channels = NotificationChannels.disabled();
        when(userSettingsService.getNotificationChannels(eq(1L), eq(NotificationType.RESIGNATION)))
                .thenReturn(channels);

        // when
        notificationEventListener.handleNotificationEvent(resignationEvent);

        // then
        verify(userSettingsService).getNotificationChannels(eq(1L), eq(NotificationType.RESIGNATION));
        verify(notificationRepository, never()).save(any(Notification.class));
    }
}
