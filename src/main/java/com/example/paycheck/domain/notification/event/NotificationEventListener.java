package com.example.paycheck.domain.notification.event;

import com.example.paycheck.domain.notification.entity.Notification;
import com.example.paycheck.domain.notification.repository.NotificationRepository;
import com.example.paycheck.domain.notification.service.SseEmitterService;
import com.example.paycheck.domain.settings.dto.NotificationChannels;
import com.example.paycheck.domain.settings.service.UserSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {
    private final NotificationRepository notificationRepository;
    private final SseEmitterService sseEmitterService;
    private final UserSettingsService userSettingsService;

    @Async
    @EventListener
    @Transactional
    public void handleNotificationEvent(NotificationEvent event) {
        Long userId = event.getUser().getId();

        log.info("알림 이벤트 처리: user={}, type={}, actionType={}",
                userId, event.getType(), event.getActionType());

        // 설정 확인 - 비활성화된 경우 조기 반환
        NotificationChannels channels = userSettingsService.getNotificationChannels(
                userId, event.getType());

        if (!channels.isShouldSend()) {
            log.info("알림 설정에 의해 전송 건너뜀: user={}, type={}", userId, event.getType());
            return;
        }

        // 알림 저장
        Notification notification = Notification.builder()
                .user(event.getUser())
                .type(event.getType())
                .title(event.getTitle())
                .actionType(event.getActionType())
                .actionData(event.getActionData())
                .isRead(false)
                .build();

        Notification savedNotification = notificationRepository.save(notification);

        // SSE를 통한 실시간 알림 전송 (push 채널이 활성화된 경우)
        if (channels.getChannels().contains("push")) {
            sseEmitterService.sendNotification(userId, savedNotification);
        }
    }
}
