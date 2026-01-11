package com.example.wagemanager.domain.worker.service;

import com.example.wagemanager.common.exception.NotFoundException;
import com.example.wagemanager.domain.user.entity.User;
import com.example.wagemanager.domain.user.enums.UserType;
import com.example.wagemanager.domain.worker.dto.WorkerDto;
import com.example.wagemanager.domain.worker.entity.Worker;
import com.example.wagemanager.domain.worker.repository.WorkerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkerService 테스트")
class WorkerServiceTest {

    @Mock
    private WorkerRepository workerRepository;

    @InjectMocks
    private WorkerService workerService;

    private Worker testWorker;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .kakaoId("test_kakao")
                .name("테스트 근로자")
                .userType(UserType.WORKER)
                .build();

        testWorker = Worker.builder()
                .id(1L)
                .user(testUser)
                .workerCode("ABC123")
                .bankName("카카오뱅크")
                .accountNumber("333312341234")
                .build();
    }

    @Test
    @DisplayName("근로자 ID로 조회 성공")
    void getWorkerById_Success() {
        // given
        when(workerRepository.findById(1L)).thenReturn(Optional.of(testWorker));

        // when
        WorkerDto.Response result = workerService.getWorkerById(1L);

        // then
        assertThat(result).isNotNull();
        verify(workerRepository).findById(1L);
    }

    @Test
    @DisplayName("근로자 ID로 조회 실패")
    void getWorkerById_NotFound() {
        // given
        when(workerRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> workerService.getWorkerById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("근로자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("사용자 ID로 근로자 조회 성공")
    void getWorkerByUserId_Success() {
        // given
        when(workerRepository.findByUserId(1L)).thenReturn(Optional.of(testWorker));

        // when
        WorkerDto.Response result = workerService.getWorkerByUserId(1L);

        // then
        assertThat(result).isNotNull();
        verify(workerRepository).findByUserId(1L);
    }

    @Test
    @DisplayName("근로자 코드로 조회 성공")
    void getWorkerByWorkerCode_Success() {
        // given
        when(workerRepository.findByWorkerCode("ABC123")).thenReturn(Optional.of(testWorker));

        // when
        WorkerDto.Response result = workerService.getWorkerByWorkerCode("ABC123");

        // then
        assertThat(result).isNotNull();
        verify(workerRepository).findByWorkerCode("ABC123");
    }

    @Test
    @DisplayName("근로자 정보 업데이트 성공")
    void updateWorker_Success() {
        // given
        WorkerDto.UpdateRequest request = WorkerDto.UpdateRequest.builder()
                .accountNumber("999988887777")
                .bankName("토스뱅크")
                .build();

        when(workerRepository.findById(1L)).thenReturn(Optional.of(testWorker));

        // when
        WorkerDto.Response result = workerService.updateWorker(1L, request);

        // then
        assertThat(result).isNotNull();
        verify(workerRepository).findById(1L);
    }

    @Test
    @DisplayName("근로자 생성 성공")
    void createWorker_Success() {
        // given
        when(workerRepository.existsByWorkerCode(anyString())).thenReturn(false);
        when(workerRepository.save(any(Worker.class))).thenReturn(testWorker);

        // when
        Worker result = workerService.createWorker(testUser, "카카오뱅크", "333312341234");

        // then
        assertThat(result).isNotNull();
        verify(workerRepository).save(any(Worker.class));
    }
}
