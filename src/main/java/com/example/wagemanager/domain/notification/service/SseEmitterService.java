package com.example.wagemanager.domain.notification.service;

import com.example.wagemanager.domain.notification.dto.NotificationResponse;
import com.example.wagemanager.domain.notification.entity.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseEmitterService {
    // 타임아웃 설정 (30분)
    private static final long DEFAULT_TIMEOUT = 30 * 60 * 1000L;

    // userId -> SseEmitter 맵핑
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 새로운 SSE 연결 생성
     */
    public SseEmitter createEmitter(Long userId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        emitters.put(userId, emitter);

        // 연결 완료 시 emitter 제거
        emitter.onCompletion(() -> {
            log.info("SSE 연결 완료: userId={}", userId);
            emitters.remove(userId);
        });

        // 타임아웃 시 emitter 제거
        emitter.onTimeout(() -> {
            log.info("SSE 타임아웃: userId={}", userId);
            emitters.remove(userId);
        });

        // 에러 발생 시 emitter 제거
        emitter.onError((e) -> {
            log.error("SSE 에러: userId={}", userId, e);
            emitters.remove(userId);
        });

        // 연결 즉시 더미 이벤트 전송 (503 에러 방지)
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("Connected to notification stream"));
        } catch (IOException e) {
            log.error("SSE 초기 메시지 전송 실패: userId={}", userId, e);
            emitters.remove(userId);
        }

        log.info("SSE 연결 생성: userId={}", userId);
        return emitter;
    }

    /**
     * 특정 사용자에게 알림 전송
     */
    public void sendNotification(Long userId, Notification notification) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                NotificationResponse response = NotificationResponse.from(notification);
                if (response != null) {
                    emitter.send(SseEmitter.event()
                            .name("notification")
                            .data(response));
                    Long notificationId = notification.getId();
                    log.info("SSE 알림 전송 성공: userId={}, notificationId={}", userId, notificationId != null ? notificationId : "N/A");
                }
            } catch (IOException e) {
                Long notificationId = notification.getId();
                log.error("SSE 알림 전송 실패: userId={}, notificationId={}", userId, notificationId != null ? notificationId : "N/A", e);
                emitters.remove(userId);
            }
        } else {
            log.debug("SSE 연결 없음: userId={}", userId);
        }
    }

    /**
     * 특정 사용자의 읽지 않은 알림 개수 전송
     */
    public void sendUnreadCount(Long userId, long count) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("unread-count")
                        .data(count));
                log.info("SSE 읽지 않은 알림 개수 전송: userId={}, count={}", userId, count);
            } catch (IOException e) {
                log.error("SSE 읽지 않은 알림 개수 전송 실패: userId={}", userId, e);
                emitters.remove(userId);
            }
        }
    }

    /**
     * 연결된 emitter 개수 조회
     */
    public int getEmitterCount() {
        return emitters.size();
    }
}
