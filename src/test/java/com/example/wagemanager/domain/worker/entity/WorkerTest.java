package com.example.wagemanager.domain.worker.entity;

import com.example.wagemanager.domain.user.entity.User;
import com.example.wagemanager.domain.user.enums.UserType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Worker 엔티티 테스트")
class WorkerTest {

    @Test
    @DisplayName("Worker 생성 성공")
    void createWorker_Success() {
        // given
        User user = User.builder()
                .id(1L)
                .kakaoId("test_kakao")
                .name("테스트 근로자")
                .userType(UserType.WORKER)
                .build();

        // when
        Worker worker = Worker.builder()
                .id(1L)
                .user(user)
                .workerCode("ABC123")
                .kakaoPayLink("https://qr.kakaopay.com/test")
                .build();

        // then
        assertThat(worker).isNotNull();
        assertThat(worker.getWorkerCode()).isEqualTo("ABC123");
        assertThat(worker.getKakaoPayLink()).isEqualTo("https://qr.kakaopay.com/test");
    }

    @Test
    @DisplayName("Worker 카카오페이 링크 업데이트")
    void updateKakaoPayLink_Success() {
        // given
        Worker worker = Worker.builder()
                .id(1L)
                .workerCode("ABC123")
                .kakaoPayLink("https://qr.kakaopay.com/old")
                .build();

        // when
        worker.updateKakaoPayLink("https://qr.kakaopay.com/new");

        // then
        assertThat(worker.getKakaoPayLink()).isEqualTo("https://qr.kakaopay.com/new");
    }
}
