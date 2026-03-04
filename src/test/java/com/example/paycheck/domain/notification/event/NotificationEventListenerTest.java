package com.example.paycheck.domain.notification.event;

import com.example.paycheck.domain.fcm.service.FcmService;
import com.example.paycheck.domain.notification.entity.Notification;
import com.example.paycheck.domain.notification.enums.NotificationActionType;
import com.example.paycheck.domain.notification.enums.NotificationType;
import com.example.paycheck.domain.notification.repository.NotificationRepository;
import com.example.paycheck.domain.notification.service.SseEmitterService;
import com.example.paycheck.domain.settings.dto.NotificationChannels;
import com.example.paycheck.domain.settings.service.UserSettingsService;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.user.enums.UserType;
import com.example.paycheck.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationEventListener 테스트")
class NotificationEventListenerTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SseEmitterService sseEmitterService;

    @Mock
    private FcmService fcmService;

    @Mock
    private UserSettingsService userSettingsService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationEventListener notificationEventListener;

    private User testUser;
    private NotificationEvent testEvent;
    private Notification savedNotification;

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

        savedNotification = Notification.builder()
                .id(1L)
                .user(testUser)
                .type(NotificationType.INVITATION)
                .title("테스트 알림")
                .actionType(NotificationActionType.VIEW_WORKPLACE_INVITATION)
                .build();

        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    }

    @Test
    @DisplayName("Push 활성화 시 SSE와 FCM 모두 전송")
    void handleNotificationEvent_ShouldSendSseAndFcm_WhenPushEnabled() {
        // given
        NotificationChannels channels = NotificationChannels.of(true, List.of("push"));
        when(userSettingsService.getNotificationChannels(eq(1L), eq(NotificationType.INVITATION)))
                .thenReturn(channels);
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        // when
        notificationEventListener.handleNotificationEvent(testEvent);

        // then
        verify(notificationRepository).save(any(Notification.class));
        verify(sseEmitterService).sendNotification(eq(1L), any(Notification.class));
        verify(fcmService).sendToUser(eq(1L), any(Notification.class));
    }

    @Test
    @DisplayName("전체 알림 비활성화 시 저장/SSE/FCM 모두 스킵")
    void handleNotificationEvent_ShouldSkipAll_WhenGlobalNotificationDisabled() {
        // given
        NotificationChannels channels = NotificationChannels.disabled();
        when(userSettingsService.getNotificationChannels(eq(1L), eq(NotificationType.INVITATION)))
                .thenReturn(channels);

        // when
        notificationEventListener.handleNotificationEvent(testEvent);

        // then
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(sseEmitterService, never()).sendNotification(anyLong(), any(Notification.class));
        verify(fcmService, never()).sendToUser(anyLong(), any(Notification.class));
    }

    @Test
    @DisplayName("특정 타입 비활성화 시 저장/전송 스킵")
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
        verify(fcmService, never()).sendToUser(anyLong(), any(Notification.class));
    }

    @Test
    @DisplayName("Push 비활성화 시 SSE와 FCM 모두 스킵 (알림 저장은 수행)")
    void handleNotificationEvent_ShouldSkipSseAndFcm_WhenPushDisabled() {
        // given
        NotificationChannels channels = NotificationChannels.of(true, List.of("email", "sms"));
        when(userSettingsService.getNotificationChannels(eq(1L), eq(NotificationType.INVITATION)))
                .thenReturn(channels);
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        // when
        notificationEventListener.handleNotificationEvent(testEvent);

        // then
        verify(notificationRepository).save(any(Notification.class));
        verify(sseEmitterService, never()).sendNotification(anyLong(), any(Notification.class));
        verify(fcmService, never()).sendToUser(anyLong(), any(Notification.class));
    }

    @Test
    @DisplayName("FCM 전송 실패해도 예외가 전파되지 않음 (알림 저장은 완료)")
    void handleNotificationEvent_ShouldNotThrow_WhenFcmFails() {
        // given
        NotificationChannels channels = NotificationChannels.of(true, List.of("push"));
        when(userSettingsService.getNotificationChannels(eq(1L), eq(NotificationType.INVITATION)))
                .thenReturn(channels);
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        doThrow(new RuntimeException("FCM 연결 실패"))
                .when(fcmService).sendToUser(anyLong(), any(Notification.class));

        // when - 예외가 전파되지 않아야 함
        notificationEventListener.handleNotificationEvent(testEvent);

        // then
        verify(notificationRepository).save(any(Notification.class));
        verify(sseEmitterService).sendNotification(eq(1L), any(Notification.class));
        verify(fcmService).sendToUser(eq(1L), any(Notification.class));
    }

    @Test
    @DisplayName("SCHEDULE_CREATED 알림 - Push 활성화 시 SSE와 FCM 전송")
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

        Notification scheduleNotification = Notification.builder()
                .id(2L)
                .user(testUser)
                .type(NotificationType.SCHEDULE_CREATED)
                .title("근무 일정이 생성되었습니다")
                .actionType(NotificationActionType.VIEW_WORK_RECORD)
                .build();
        when(notificationRepository.save(any(Notification.class))).thenReturn(scheduleNotification);

        // when
        notificationEventListener.handleNotificationEvent(scheduleEvent);

        // then
        verify(notificationRepository).save(any(Notification.class));
        verify(sseEmitterService).sendNotification(eq(1L), any(Notification.class));
        verify(fcmService).sendToUser(eq(1L), any(Notification.class));
    }

    @Test
    @DisplayName("RESIGNATION 알림 - 비활성화 시 스킵")
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
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(fcmService, never()).sendToUser(anyLong(), any(Notification.class));
    }

    @Test
    @DisplayName("사용자를 찾을 수 없으면 저장/전송 스킵")
    void handleNotificationEvent_ShouldSkip_WhenUserNotFound() {
        // given
        User unknownUser = User.builder()
                .id(999L)
                .kakaoId("unknown")
                .name("없는 사용자")
                .userType(UserType.WORKER)
                .build();

        NotificationEvent eventForUnknown = NotificationEvent.builder()
                .user(unknownUser)
                .type(NotificationType.INVITATION)
                .title("테스트 알림")
                .actionType(NotificationActionType.VIEW_WORKPLACE_INVITATION)
                .build();

        NotificationChannels channels = NotificationChannels.of(true, List.of("push"));
        when(userSettingsService.getNotificationChannels(eq(999L), eq(NotificationType.INVITATION)))
                .thenReturn(channels);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // when
        notificationEventListener.handleNotificationEvent(eventForUnknown);

        // then
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(sseEmitterService, never()).sendNotification(anyLong(), any(Notification.class));
        verify(fcmService, never()).sendToUser(anyLong(), any(Notification.class));
    }
}
