package com.example.wagemanager.domain.notification.entity;

import com.example.wagemanager.domain.notification.enums.NotificationActionType;
import com.example.wagemanager.domain.notification.enums.NotificationType;
import com.example.wagemanager.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Notification 엔티티 테스트")
class NotificationTest {

    private Notification notification;
    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = mock(User.class);
        notification = Notification.builder()
                .id(1L)
                .user(mockUser)
                .type(NotificationType.SCHEDULE_CREATED)
                .title("근무 일정이 등록되었습니다")
                .actionType(NotificationActionType.NONE)
                .isRead(false)
                .build();
    }

    @Test
    @DisplayName("알림 읽음 처리")
    void markAsRead() {
        // when
        notification.markAsRead();

        // then
        assertThat(notification.getIsRead()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();
    }

    @Test
    @DisplayName("알림 읽음 처리 - 이미 읽은 알림")
    void markAsRead_AlreadyRead() {
        // given
        notification.markAsRead();
        var firstReadAt = notification.getReadAt();

        // when
        notification.markAsRead();

        // then
        assertThat(notification.getIsRead()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();
        assertThat(notification.getReadAt()).isAfterOrEqualTo(firstReadAt);
    }
}
