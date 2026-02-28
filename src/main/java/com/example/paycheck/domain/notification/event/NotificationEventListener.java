package com.example.paycheck.domain.notification.event;

import com.example.paycheck.domain.notification.entity.Notification;
import com.example.paycheck.domain.notification.repository.NotificationRepository;
import com.example.paycheck.domain.notification.service.SseEmitterService;
import com.example.paycheck.domain.settings.dto.NotificationChannels;
import com.example.paycheck.domain.settings.enums.NotificationChannel;
import com.example.paycheck.domain.settings.service.UserSettingsService;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {
    private final NotificationRepository notificationRepository;
    private final SseEmitterService sseEmitterService;
    private final UserSettingsService userSettingsService;
    private final UserRepository userRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleNotificationEvent(NotificationEvent event) {
        Long userId = event.getUser().getId();

        log.info("알림 이벤트 처리: user={}, type={}, actionType={}",
                userId, event.getType(), event.getActionType());

        // 과거 데이터/직접 시드 데이터 등으로 설정이 없는 사용자를 방어
        // 기본 설정을 생성해 알림이 불필요하게 드롭되는 상황을 방지한다.
        userSettingsService.getOrCreateSettings(userId);

        // 설정 확인 - 비활성화된 경우 조기 반환
        NotificationChannels channels = userSettingsService.getNotificationChannels(
                userId, event.getType());

        if (!channels.isShouldSend()) {
            log.info("알림 설정에 의해 전송 건너뜀: user={}, type={}", userId, event.getType());
            return;
        }

        User recipient = userRepository.findById(userId).orElse(null);
        if (recipient == null) {
            log.warn("알림 대상 사용자를 찾을 수 없어 저장 건너뜀: userId={}, type={}", userId, event.getType());
            return;
        }

        try {
            // 알림 저장
            Notification notification = Notification.builder()
                    .user(recipient)
                    .type(event.getType())
                    .title(event.getTitle())
                    .actionType(event.getActionType())
                    .actionData(event.getActionData())
                    .isRead(false)
                    .build();

            Notification savedNotification = notificationRepository.save(notification);

            // SSE를 통한 실시간 알림 전송 (push 채널이 활성화된 경우)
            if (channels.getChannels().contains(NotificationChannel.PUSH.getValue())) {
                sseEmitterService.sendNotification(userId, savedNotification);
            }
        } catch (Exception e) {
            log.error("알림 저장/전송 실패: userId={}, type={}, actionType={}",
                    userId, event.getType(), event.getActionType(), e);
            throw e;
        }
    }
}
