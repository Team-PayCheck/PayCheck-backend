package com.example.wagemanager.domain.notification.event;

import com.example.wagemanager.domain.notification.entity.Notification;
import com.example.wagemanager.domain.notification.repository.NotificationRepository;
import com.example.wagemanager.domain.notification.service.SseEmitterService;
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

    @Async
    @EventListener
    @Transactional
    public void handleNotificationEvent(NotificationEvent event) {
        log.info("알림 이벤트 처리: user={}, type={}, actionType={}",
                event.getUser().getId(), event.getType(), event.getActionType());

        // 알림 저장
        Notification notification = Notification.builder()
                .user(event.getUser())
                .type(event.getType())
                .title(event.getTitle())
                .actionType(event.getActionType())
                .actionData(event.getActionData())
                .isRead(false)
                .build();

        if (notification != null) {
            Notification savedNotification = notificationRepository.save(notification);

            // SSE를 통한 실시간 알림 전송
            if (savedNotification != null) {
                sseEmitterService.sendNotification(event.getUser().getId(), savedNotification);
            }
        }
    }
}
