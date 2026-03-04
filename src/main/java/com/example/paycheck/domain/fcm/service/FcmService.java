package com.example.paycheck.domain.fcm.service;

import com.example.paycheck.domain.notification.entity.Notification;
import com.example.paycheck.domain.notification.enums.NotificationType;
import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FcmService {

    private final FcmTokenService fcmTokenService;
    private final FirebaseMessaging firebaseMessaging;

    public FcmService(FcmTokenService fcmTokenService, @Nullable FirebaseMessaging firebaseMessaging) {
        this.fcmTokenService = fcmTokenService;
        this.firebaseMessaging = firebaseMessaging;
    }

    public void sendToUser(Long userId, Notification notification) {
        if (firebaseMessaging == null) {
            log.debug("FirebaseMessaging이 초기화되지 않아 FCM 전송을 건너뜁니다");
            return;
        }

        List<String> tokens = fcmTokenService.getTokensByUserId(userId);
        if (tokens.isEmpty()) {
            log.debug("FCM 토큰 없음: userId={}", userId);
            return;
        }

        Map<String, String> data = buildDataPayload(notification);

        if (tokens.size() == 1) {
            sendToSingleDevice(tokens.get(0), notification, data, userId);
        } else {
            sendToMultipleDevices(tokens, notification, data, userId);
        }
    }

    private void sendToSingleDevice(String token, Notification notification,
                                     Map<String, String> data, Long userId) {
        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(buildFcmNotification(notification))
                    .putAllData(data)
                    .setAndroidConfig(buildAndroidConfig())
                    .setApnsConfig(buildApnsConfig())
                    .build();

            String messageId = firebaseMessaging.send(message);
            log.info("FCM 전송 성공: userId={}, messageId={}", userId, messageId);
        } catch (FirebaseMessagingException e) {
            handleSendError(e, token, userId);
        }
    }

    private void sendToMultipleDevices(List<String> tokens, Notification notification,
                                        Map<String, String> data, Long userId) {
        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(buildFcmNotification(notification))
                .putAllData(data)
                .setAndroidConfig(buildAndroidConfig())
                .setApnsConfig(buildApnsConfig())
                .build();

        try {
            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
            log.info("FCM 멀티캐스트 전송: userId={}, success={}, failure={}",
                    userId, response.getSuccessCount(), response.getFailureCount());

            handleMulticastErrors(response, tokens);
        } catch (FirebaseMessagingException e) {
            log.error("FCM 멀티캐스트 전송 실패: userId={}", userId, e);
        }
    }

    private com.google.firebase.messaging.Notification buildFcmNotification(
            Notification notification) {
        return com.google.firebase.messaging.Notification.builder()
                .setTitle(getNotificationCategoryTitle(notification.getType()))
                .setBody(notification.getTitle())
                .build();
    }

    private Map<String, String> buildDataPayload(Notification notification) {
        Map<String, String> data = new HashMap<>();
        data.put("notificationId", String.valueOf(notification.getId()));
        data.put("type", notification.getType().name());
        data.put("actionType", notification.getActionType().name());
        if (notification.getActionData() != null) {
            data.put("actionData", notification.getActionData());
        }
        return data;
    }

    private AndroidConfig buildAndroidConfig() {
        return AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .setNotification(AndroidNotification.builder()
                        .setChannelId("paycheck_notifications")
                        .build())
                .build();
    }

    private ApnsConfig buildApnsConfig() {
        return ApnsConfig.builder()
                .setAps(Aps.builder()
                        .setSound("default")
                        .setBadge(1)
                        .build())
                .build();
    }

    private String getNotificationCategoryTitle(NotificationType type) {
        return switch (type) {
            case SCHEDULE_CHANGE, SCHEDULE_CREATED, SCHEDULE_APPROVAL_REQUEST,
                 SCHEDULE_APPROVED, SCHEDULE_REJECTED, SCHEDULE_DELETED
                    -> "근무 일정";
            case CORRECTION_RESPONSE, UNREAD_CORRECTION_REQUEST
                    -> "정정 요청";
            case PAYMENT_DUE, PAYMENT_SUCCESS, PAYMENT_FAILED,
                 WORK_RECORD_CONFIRMATION
                    -> "급여";
            case INVITATION -> "초대";
            case RESIGNATION -> "퇴사";
            case NOTICE_CREATED -> "공지사항";
        };
    }

    private void handleSendError(FirebaseMessagingException e, String token, Long userId) {
        MessagingErrorCode errorCode = e.getMessagingErrorCode();
        if (errorCode == MessagingErrorCode.UNREGISTERED ||
            errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
            log.warn("무효한 FCM 토큰 삭제: userId={}, errorCode={}", userId, errorCode);
            fcmTokenService.deleteToken(token);
        } else {
            log.error("FCM 전송 실패: userId={}, errorCode={}", userId, errorCode, e);
        }
    }

    private void handleMulticastErrors(BatchResponse response, List<String> tokens) {
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResponse = responses.get(i);
            if (!sendResponse.isSuccessful()) {
                FirebaseMessagingException exception = sendResponse.getException();
                if (exception != null) {
                    MessagingErrorCode errorCode = exception.getMessagingErrorCode();
                    if (errorCode == MessagingErrorCode.UNREGISTERED ||
                        errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                        fcmTokenService.deleteToken(tokens.get(i));
                        log.warn("무효한 FCM 토큰 삭제: token index={}", i);
                    }
                }
            }
        }
    }
}
