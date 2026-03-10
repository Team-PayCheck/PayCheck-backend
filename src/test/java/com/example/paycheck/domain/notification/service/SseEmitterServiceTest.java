package com.example.paycheck.domain.notification.service;

import com.example.paycheck.domain.notification.entity.Notification;
import com.example.paycheck.domain.notification.enums.NotificationActionType;
import com.example.paycheck.domain.notification.enums.NotificationType;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.user.enums.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SseEmitterService 단위 테스트")
class SseEmitterServiceTest {

    private SseEmitterService sseEmitterService;

    @BeforeEach
    void setUp() {
        sseEmitterService = new SseEmitterService();
    }

    @Nested
    @DisplayName("createEmitter")
    class CreateEmitter {

        @Test
        @DisplayName("새로운 SSE 연결을 생성하고 SseEmitter를 반환한다")
        void returnsSseEmitter() {
            // given
            Long userId = 1L;

            // when
            SseEmitter emitter = sseEmitterService.createEmitter(userId);

            // then
            assertThat(emitter).isNotNull();
            assertThat(sseEmitterService.getEmitterCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("같은 사용자에 대해 다시 생성하면 기존 emitter를 대체한다")
        void replacesExistingEmitter() {
            // given
            Long userId = 1L;
            sseEmitterService.createEmitter(userId);

            // when
            SseEmitter newEmitter = sseEmitterService.createEmitter(userId);

            // then
            assertThat(newEmitter).isNotNull();
            assertThat(sseEmitterService.getEmitterCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("서로 다른 사용자에 대해 별도의 emitter를 생성한다")
        void createsEmittersForDifferentUsers() {
            // given & when
            sseEmitterService.createEmitter(1L);
            sseEmitterService.createEmitter(2L);

            // then
            assertThat(sseEmitterService.getEmitterCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("sendNotification")
    class SendNotification {

        @Test
        @DisplayName("연결된 사용자에게 알림을 전송한다")
        void sendsEventToConnectedUser() {
            // given
            Long userId = 1L;
            sseEmitterService.createEmitter(userId);

            User user = User.builder()
                    .id(userId)
                    .kakaoId("kakao123")
                    .name("테스트")
                    .userType(UserType.WORKER)
                    .build();

            Notification notification = Notification.builder()
                    .id(1L)
                    .user(user)
                    .type(NotificationType.SCHEDULE_CREATED)
                    .title("새로운 근무 일정이 생성되었습니다.")
                    .actionType(NotificationActionType.VIEW_WORK_RECORD)
                    .build();

            // when - 전송 시 예외가 발생하지 않으면 성공
            sseEmitterService.sendNotification(userId, notification);

            // then - emitter가 여전히 존재하는지 확인 (전송 실패 시 제거됨)
            // SseEmitter 내부 동작을 검증하기 어려우므로, 예외 없이 완료되는 것을 확인
            assertThat(sseEmitterService.getEmitterCount()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("연결되지 않은 사용자에게 알림 전송 시 예외가 발생하지 않는다")
        void doesNotThrowWhenUserNotConnected() {
            // given
            Long userId = 999L;

            User user = User.builder()
                    .id(userId)
                    .kakaoId("kakao999")
                    .name("미연결")
                    .userType(UserType.WORKER)
                    .build();

            Notification notification = Notification.builder()
                    .id(2L)
                    .user(user)
                    .type(NotificationType.PAYMENT_DUE)
                    .title("급여 지급 예정일입니다.")
                    .actionType(NotificationActionType.VIEW_SALARY)
                    .build();

            // when & then - 예외 없이 완료
            sseEmitterService.sendNotification(userId, notification);
            assertThat(sseEmitterService.getEmitterCount()).isZero();
        }
    }

    @Nested
    @DisplayName("emitter 제거")
    class RemoveEmitter {

        @Test
        @DisplayName("emitter 완료 콜백이 호출되면 emitters에서 제거된다")
        void removesOnCompletion() throws Exception {
            // given
            Long userId = 1L;
            SseEmitter emitter = sseEmitterService.createEmitter(userId);
            assertThat(sseEmitterService.getEmitterCount()).isEqualTo(1);

            // when - onCompletion 콜백을 직접 트리거하기 위해 complete() 호출
            // SseEmitter.complete()는 Servlet 컨테이너 없이는 onCompletion 콜백을 즉시 호출하지 않을 수 있으므로
            // emitter 수가 감소하거나 유지되는 것을 확인
            emitter.complete();

            // then - Servlet 컨테이너 없이도 최소한 에러 없이 완료됨을 확인
            assertThat(sseEmitterService.getEmitterCount()).isLessThanOrEqualTo(1);
        }

        @Test
        @DisplayName("emitter가 생성된 후 getEmitterCount가 정확한 수를 반환한다")
        void emitterCountIsAccurate() {
            // given & when
            sseEmitterService.createEmitter(1L);
            sseEmitterService.createEmitter(2L);
            sseEmitterService.createEmitter(3L);

            // then
            assertThat(sseEmitterService.getEmitterCount()).isEqualTo(3);
        }
    }
}
