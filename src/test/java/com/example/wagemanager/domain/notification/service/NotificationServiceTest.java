package com.example.wagemanager.domain.notification.service;

import com.example.wagemanager.domain.notification.dto.NotificationPageResponse;
import com.example.wagemanager.domain.notification.dto.NotificationResponse;
import com.example.wagemanager.domain.notification.entity.Notification;
import com.example.wagemanager.domain.notification.enums.NotificationType;
import com.example.wagemanager.domain.notification.repository.NotificationRepository;
import com.example.wagemanager.domain.user.entity.User;
import com.example.wagemanager.domain.user.enums.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService 테스트")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User testUser;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .kakaoId("test_kakao")
                .name("테스트 사용자")
                .userType(UserType.WORKER)
                .build();

        testNotification = Notification.builder()
                .id(1L)
                .user(testUser)
                .type(NotificationType.PAYMENT_SUCCESS)
                .title("급여 입금 완료")
                .isRead(false)
                .build();
    }

    @Test
    @DisplayName("사용자 알림 목록 조회 성공")
    void getUserNotifications_Success() {
        // given
        List<Notification> notifications = Arrays.asList(testNotification);
        when(notificationRepository.findByUserOrderByCreatedAtDesc(testUser))
                .thenReturn(notifications);

        // when
        List<Notification> result = notificationService.getUserNotifications(testUser);

        // then
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(1);
        verify(notificationRepository).findByUserOrderByCreatedAtDesc(testUser);
    }

    @Test
    @DisplayName("사용자 알림 DTO 목록 조회 성공")
    void getUserNotificationDtos_Success() {
        // given
        List<Notification> notifications = Arrays.asList(testNotification);
        when(notificationRepository.findByUserOrderByCreatedAtDesc(testUser))
                .thenReturn(notifications);

        // when
        List<NotificationResponse> result = notificationService.getUserNotificationDtos(testUser);

        // then
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(1);
        verify(notificationRepository).findByUserOrderByCreatedAtDesc(testUser);
    }

    @Test
    @DisplayName("페이징된 알림 목록 조회 성공")
    void getNotifications_Success() {
        // given
        Page<Notification> page = new PageImpl<>(Arrays.asList(testNotification));
        when(notificationRepository.findByUser(eq(testUser), any(Pageable.class)))
                .thenReturn(page);
        when(notificationRepository.countByUserAndIsReadFalse(testUser)).thenReturn(1L);

        // when
        NotificationPageResponse result = notificationService.getNotifications(testUser, null, 1, 10);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getNotifications()).isNotEmpty();
        assertThat(result.getUnreadCount()).isEqualTo(1L);
        verify(notificationRepository).findByUser(eq(testUser), any(Pageable.class));
    }

    @Test
    @DisplayName("읽지 않은 알림만 조회 성공")
    void getNotifications_UnreadOnly() {
        // given
        Page<Notification> page = new PageImpl<>(Arrays.asList(testNotification));
        when(notificationRepository.findByUserAndIsRead(eq(testUser), eq(false), any(Pageable.class)))
                .thenReturn(page);
        when(notificationRepository.countByUserAndIsReadFalse(testUser)).thenReturn(1L);

        // when
        NotificationPageResponse result = notificationService.getNotifications(testUser, false, 1, 10);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getNotifications()).isNotEmpty();
        verify(notificationRepository).findByUserAndIsRead(eq(testUser), eq(false), any(Pageable.class));
    }

    @Test
    @DisplayName("읽지 않은 알림 수 조회 성공")
    void getUnreadCount_Success() {
        // given
        when(notificationRepository.countByUserAndIsReadFalse(testUser)).thenReturn(5L);

        // when
        long result = notificationService.getUnreadCount(testUser);

        // then
        assertThat(result).isEqualTo(5L);
        verify(notificationRepository).countByUserAndIsReadFalse(testUser);
    }

    @Test
    @DisplayName("읽지 않은 알림 목록 조회 성공")
    void getUnreadNotifications_Success() {
        // given
        List<Notification> notifications = Arrays.asList(testNotification);
        when(notificationRepository.findUnreadByUser(testUser)).thenReturn(notifications);

        // when
        List<Notification> result = notificationService.getUnreadNotifications(testUser);

        // then
        assertThat(result).isNotEmpty();
        verify(notificationRepository).findUnreadByUser(testUser);
    }

    @Test
    @DisplayName("읽지 않은 알림 DTO 목록 조회 성공")
    void getUnreadNotificationDtos_Success() {
        // given
        List<Notification> notifications = Arrays.asList(testNotification);
        when(notificationRepository.findUnreadByUser(testUser)).thenReturn(notifications);

        // when
        List<NotificationResponse> result = notificationService.getUnreadNotificationDtos(testUser);

        // then
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(1);
        verify(notificationRepository).findUnreadByUser(testUser);
    }

    @Test
    @DisplayName("알림 읽음 처리 성공")
    void markAsRead_Success() {
        // given
        Notification notification = mock(Notification.class);

        // when
        notificationService.markAsRead(notification);

        // then
        verify(notification).markAsRead();
    }

    @Test
    @DisplayName("ID로 알림 읽음 처리 실패 - 알림 없음")
    void markAsReadById_Fail_NotFound() {
        // given
        when(notificationRepository.findByIdAndUser(999L, testUser)).thenReturn(java.util.Optional.empty());

        // when & then
        assertThatThrownBy(() -> notificationService.markAsReadById(testUser, 999L))
                .isInstanceOf(com.example.wagemanager.common.exception.NotFoundException.class)
                .hasMessageContaining("알림을 찾을 수 없습니다");
        verify(notificationRepository).findByIdAndUser(999L, testUser);
    }

    @Test
    @DisplayName("ID로 알림 읽음 처리 성공")
    void markAsReadById_Success() {
        // given
        Notification notification = mock(Notification.class);
        when(notificationRepository.findByIdAndUser(1L, testUser)).thenReturn(java.util.Optional.of(notification));

        // when
        notificationService.markAsReadById(testUser, 1L);

        // then
        verify(notification).markAsRead();
        verify(notificationRepository).findByIdAndUser(1L, testUser);
    }

    @Test
    @DisplayName("모든 알림 읽음 처리 성공")
    void markAllAsRead_Success() {
        // when
        notificationService.markAllAsRead(testUser);

        // then
        verify(notificationRepository).markAllAsReadByUser(eq(testUser), any(java.time.LocalDateTime.class));
    }

    @Test
    @DisplayName("알림 삭제 실패 - 알림 없음")
    void deleteNotification_Fail_NotFound() {
        // given
        when(notificationRepository.findByIdAndUser(999L, testUser)).thenReturn(java.util.Optional.empty());

        // when & then
        assertThatThrownBy(() -> notificationService.deleteNotification(testUser, 999L))
                .isInstanceOf(com.example.wagemanager.common.exception.NotFoundException.class)
                .hasMessageContaining("알림을 찾을 수 없습니다");
        verify(notificationRepository).findByIdAndUser(999L, testUser);
    }

    @Test
    @DisplayName("알림 삭제 성공")
    void deleteNotification_Success() {
        // given
        Notification notification = mock(Notification.class);
        when(notificationRepository.findByIdAndUser(1L, testUser)).thenReturn(java.util.Optional.of(notification));

        // when
        notificationService.deleteNotification(testUser, 1L);

        // then
        verify(notificationRepository).findByIdAndUser(1L, testUser);
        verify(notificationRepository).delete(notification);
    }
}
