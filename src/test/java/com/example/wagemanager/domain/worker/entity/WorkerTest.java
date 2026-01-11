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
                .accountNumber("1111-2222-3333")
                .bankName("카카오뱅크")
                .build();

        // then
        assertThat(worker).isNotNull();
        assertThat(worker.getWorkerCode()).isEqualTo("ABC123");
        assertThat(worker.getAccountNumber()).isEqualTo("1111-2222-3333");
        assertThat(worker.getBankName()).isEqualTo("카카오뱅크");
    }

    @Test
    @DisplayName("Worker 계좌 정보 업데이트")
    void updateAccount_Success() {
        // given
        Worker worker = Worker.builder()
                .id(1L)
                .workerCode("ABC123")
                .accountNumber("1111-2222-3333")
                .bankName("카카오뱅크")
                .build();

        // when
        worker.updateAccount("9999-8888-7777", "토스뱅크");

        // then
        assertThat(worker.getAccountNumber()).isEqualTo("9999-8888-7777");
        assertThat(worker.getBankName()).isEqualTo("토스뱅크");
    }
}
