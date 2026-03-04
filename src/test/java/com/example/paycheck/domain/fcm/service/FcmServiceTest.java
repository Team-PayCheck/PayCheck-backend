package com.example.paycheck.domain.fcm.service;

import com.example.paycheck.domain.notification.entity.Notification;
import com.example.paycheck.domain.notification.enums.NotificationActionType;
import com.example.paycheck.domain.notification.enums.NotificationType;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.user.enums.UserType;
import com.google.firebase.messaging.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FcmService 테스트")
class FcmServiceTest {

    @Mock
    private FcmTokenService fcmTokenService;

    @Mock
    private FirebaseMessaging firebaseMessaging;

    private FcmService fcmService;
    private FcmService fcmServiceWithoutFirebase;

    private User testUser;
    private Notification testNotification;
    private Notification notificationWithoutActionData;

    @BeforeEach
    void setUp() {
        fcmService = new FcmService(fcmTokenService, firebaseMessaging);
        fcmServiceWithoutFirebase = new FcmService(fcmTokenService, null);

        testUser = User.builder()
                .id(1L)
                .kakaoId("test_kakao")
                .name("테스트 사용자")
                .userType(UserType.WORKER)
                .build();

        testNotification = Notification.builder()
                .id(1L)
                .user(testUser)
                .type(NotificationType.INVITATION)
                .title("근무지 초대가 도착했습니다")
                .actionType(NotificationActionType.VIEW_WORKPLACE_INVITATION)
                .actionData("{\"workplaceId\": 1}")
                .build();

        notificationWithoutActionData = Notification.builder()
                .id(2L)
                .user(testUser)
                .type(NotificationType.RESIGNATION)
                .title("퇴사 처리되었습니다")
                .actionType(NotificationActionType.NONE)
                .actionData(null)
                .build();
    }

    @Test
    @DisplayName("FirebaseMessaging이 null이면 FCM 전송을 건너뜀")
    void sendToUser_ShouldSkip_WhenFirebaseMessagingIsNull() {
        // when
        fcmServiceWithoutFirebase.sendToUser(1L, testNotification);

        // then
        verifyNoInteractions(fcmTokenService);
        verifyNoInteractions(firebaseMessaging);
    }

    @Test
    @DisplayName("토큰이 없으면 FCM 전송하지 않음")
    void sendToUser_ShouldSkip_WhenNoTokens() {
        // given
        when(fcmTokenService.getTokensByUserId(1L)).thenReturn(Collections.emptyList());

        // when
        fcmService.sendToUser(1L, testNotification);

        // then
        verifyNoInteractions(firebaseMessaging);
    }

    @Test
    @DisplayName("단일 디바이스에 FCM 전송 성공")
    void sendToUser_ShouldSendSingleMessage_WhenOneToken() throws Exception {
        // given
        when(fcmTokenService.getTokensByUserId(1L)).thenReturn(List.of("token_1"));
        when(firebaseMessaging.send(any(Message.class))).thenReturn("message_id_1");

        // when
        fcmService.sendToUser(1L, testNotification);

        // then
        verify(firebaseMessaging).send(any(Message.class));
        verify(firebaseMessaging, never()).sendEachForMulticast(any(MulticastMessage.class));
    }

    @Test
    @DisplayName("actionData가 null인 알림도 정상 전송")
    void sendToUser_ShouldSend_WhenActionDataIsNull() throws Exception {
        // given
        when(fcmTokenService.getTokensByUserId(1L)).thenReturn(List.of("token_1"));
        when(firebaseMessaging.send(any(Message.class))).thenReturn("message_id_2");

        // when
        fcmService.sendToUser(1L, notificationWithoutActionData);

        // then
        verify(firebaseMessaging).send(any(Message.class));
    }

    @Test
    @DisplayName("복수 디바이스에 FCM 멀티캐스트 전송")
    void sendToUser_ShouldSendMulticast_WhenMultipleTokens() throws Exception {
        // given
        when(fcmTokenService.getTokensByUserId(1L)).thenReturn(List.of("token_1", "token_2"));

        SendResponse successResponse = mock(SendResponse.class);
        when(successResponse.isSuccessful()).thenReturn(true);

        BatchResponse batchResponse = mock(BatchResponse.class);
        when(batchResponse.getSuccessCount()).thenReturn(2);
        when(batchResponse.getFailureCount()).thenReturn(0);
        when(batchResponse.getResponses()).thenReturn(List.of(successResponse, successResponse));
        when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batchResponse);

        // when
        fcmService.sendToUser(1L, testNotification);

        // then
        verify(firebaseMessaging).sendEachForMulticast(any(MulticastMessage.class));
        verify(firebaseMessaging, never()).send(any(Message.class));
        verify(fcmTokenService, never()).deleteToken(anyString());
    }

    @Test
    @DisplayName("단일 전송 실패 - UNREGISTERED 에러 시 토큰 삭제")
    void sendToUser_ShouldDeleteToken_WhenUnregistered() throws Exception {
        // given
        when(fcmTokenService.getTokensByUserId(1L)).thenReturn(List.of("invalid_token"));

        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);
        when(firebaseMessaging.send(any(Message.class))).thenThrow(exception);

        // when
        fcmService.sendToUser(1L, testNotification);

        // then
        verify(fcmTokenService).deleteToken("invalid_token");
    }

    @Test
    @DisplayName("단일 전송 실패 - INVALID_ARGUMENT 에러 시 토큰 삭제")
    void sendToUser_ShouldDeleteToken_WhenInvalidArgument() throws Exception {
        // given
        when(fcmTokenService.getTokensByUserId(1L)).thenReturn(List.of("bad_token"));

        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.INVALID_ARGUMENT);
        when(firebaseMessaging.send(any(Message.class))).thenThrow(exception);

        // when
        fcmService.sendToUser(1L, testNotification);

        // then
        verify(fcmTokenService).deleteToken("bad_token");
    }

    @Test
    @DisplayName("단일 전송 실패 - INTERNAL 에러 시 토큰 삭제하지 않음")
    void sendToUser_ShouldNotDeleteToken_WhenInternalError() throws Exception {
        // given
        when(fcmTokenService.getTokensByUserId(1L)).thenReturn(List.of("valid_token"));

        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.INTERNAL);
        when(firebaseMessaging.send(any(Message.class))).thenThrow(exception);

        // when
        fcmService.sendToUser(1L, testNotification);

        // then
        verify(fcmTokenService, never()).deleteToken(anyString());
    }

    @Test
    @DisplayName("멀티캐스트 - 일부 토큰만 실패 시 실패한 토큰만 삭제")
    void sendToUser_ShouldDeleteOnlyFailedTokens_WhenPartialMulticastFailure() throws Exception {
        // given
        when(fcmTokenService.getTokensByUserId(1L)).thenReturn(List.of("good_token", "expired_token", "good_token_2"));

        SendResponse successResponse = mock(SendResponse.class);
        when(successResponse.isSuccessful()).thenReturn(true);

        FirebaseMessagingException unregisteredException = mock(FirebaseMessagingException.class);
        when(unregisteredException.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);

        SendResponse failedResponse = mock(SendResponse.class);
        when(failedResponse.isSuccessful()).thenReturn(false);
        when(failedResponse.getException()).thenReturn(unregisteredException);

        BatchResponse batchResponse = mock(BatchResponse.class);
        when(batchResponse.getSuccessCount()).thenReturn(2);
        when(batchResponse.getFailureCount()).thenReturn(1);
        when(batchResponse.getResponses()).thenReturn(List.of(successResponse, failedResponse, successResponse));
        when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batchResponse);

        // when
        fcmService.sendToUser(1L, testNotification);

        // then - 두 번째 토큰(expired_token)만 삭제
        verify(fcmTokenService).deleteToken("expired_token");
        verify(fcmTokenService, never()).deleteToken("good_token");
        verify(fcmTokenService, never()).deleteToken("good_token_2");
    }

    @Test
    @DisplayName("멀티캐스트 - 삭제 불필요한 에러(INTERNAL)는 토큰 유지")
    void sendToUser_ShouldNotDeleteToken_WhenMulticastNonDeletableError() throws Exception {
        // given
        when(fcmTokenService.getTokensByUserId(1L)).thenReturn(List.of("token_1", "token_2"));

        FirebaseMessagingException internalException = mock(FirebaseMessagingException.class);
        when(internalException.getMessagingErrorCode()).thenReturn(MessagingErrorCode.INTERNAL);

        SendResponse successResponse = mock(SendResponse.class);
        when(successResponse.isSuccessful()).thenReturn(true);

        SendResponse failedResponse = mock(SendResponse.class);
        when(failedResponse.isSuccessful()).thenReturn(false);
        when(failedResponse.getException()).thenReturn(internalException);

        BatchResponse batchResponse = mock(BatchResponse.class);
        when(batchResponse.getSuccessCount()).thenReturn(1);
        when(batchResponse.getFailureCount()).thenReturn(1);
        when(batchResponse.getResponses()).thenReturn(List.of(successResponse, failedResponse));
        when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batchResponse);

        // when
        fcmService.sendToUser(1L, testNotification);

        // then
        verify(fcmTokenService, never()).deleteToken(anyString());
    }

    @Test
    @DisplayName("멀티캐스트 전체 실패 (FirebaseMessagingException) 시 예외 전파되지 않음")
    void sendToUser_ShouldNotThrow_WhenMulticastCompletelyFails() throws Exception {
        // given
        when(fcmTokenService.getTokensByUserId(1L)).thenReturn(List.of("token_1", "token_2"));
        when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class)))
                .thenThrow(mock(FirebaseMessagingException.class));

        // when - 예외가 전파되지 않아야 함
        fcmService.sendToUser(1L, testNotification);

        // then
        verify(firebaseMessaging).sendEachForMulticast(any(MulticastMessage.class));
    }

    @Test
    @DisplayName("다양한 NotificationType에 대해 정상 전송")
    void sendToUser_ShouldWork_ForDifferentNotificationTypes() throws Exception {
        // given - PAYMENT_SUCCESS 타입
        Notification paymentNotification = Notification.builder()
                .id(3L)
                .user(testUser)
                .type(NotificationType.PAYMENT_SUCCESS)
                .title("급여가 입금되었습니다")
                .actionType(NotificationActionType.VIEW_SALARY)
                .actionData("{\"salaryId\": 5}")
                .build();

        when(fcmTokenService.getTokensByUserId(1L)).thenReturn(List.of("token_1"));
        when(firebaseMessaging.send(any(Message.class))).thenReturn("message_id_3");

        // when
        fcmService.sendToUser(1L, paymentNotification);

        // then
        verify(firebaseMessaging).send(any(Message.class));
    }
}
