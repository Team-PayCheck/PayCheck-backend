package com.example.paycheck.domain.user.service;

import com.example.paycheck.common.exception.BadRequestException;
import com.example.paycheck.common.exception.NotFoundException;
import com.example.paycheck.domain.auth.repository.RefreshTokenRepository;
import com.example.paycheck.domain.contract.entity.WorkerContract;
import com.example.paycheck.domain.contract.repository.WorkerContractRepository;
import com.example.paycheck.domain.employer.entity.Employer;
import com.example.paycheck.domain.employer.repository.EmployerRepository;
import com.example.paycheck.domain.fcm.repository.FcmTokenRepository;
import com.example.paycheck.domain.notification.repository.NotificationRepository;
import com.example.paycheck.domain.allowance.repository.WeeklyAllowanceRepository;
import com.example.paycheck.domain.allowance.service.WeeklyAllowanceService;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.user.repository.UserRepository;
import com.example.paycheck.domain.user.enums.UserType;
import com.example.paycheck.domain.worker.entity.Worker;
import com.example.paycheck.domain.worker.repository.WorkerRepository;
import com.example.paycheck.domain.workplace.entity.Workplace;
import com.example.paycheck.domain.workplace.repository.WorkplaceRepository;
import com.example.paycheck.domain.workrecord.enums.WorkRecordStatus;
import com.example.paycheck.domain.workrecord.repository.WorkRecordRepository;
import com.example.paycheck.domain.workrecord.service.WorkRecordCoordinatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserWithdrawService 테스트")
class UserWithdrawServiceTest {

    @Mock
    private EmployerRepository employerRepository;
    @Mock
    private WorkerRepository workerRepository;
    @Mock
    private WorkplaceRepository workplaceRepository;
    @Mock
    private WorkerContractRepository workerContractRepository;
    @Mock
    private WorkRecordRepository workRecordRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private FcmTokenRepository fcmTokenRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private WeeklyAllowanceService weeklyAllowanceService;
    @Mock
    private WeeklyAllowanceRepository weeklyAllowanceRepository;
    @Mock
    private WorkRecordCoordinatorService workRecordCoordinatorService;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserWithdrawService userWithdrawService;

    private User employer;
    private User worker;

    @BeforeEach
    void setUp() {
        employer = User.builder()
                .id(1L)
                .kakaoId("kakao_employer")
                .name("고용주")
                .userType(UserType.EMPLOYER)
                .build();

        worker = User.builder()
                .id(2L)
                .kakaoId("kakao_worker")
                .name("근로자")
                .userType(UserType.WORKER)
                .build();
    }

    @Test
    @DisplayName("고용주 탈퇴 성공 - Workplace 비활성화, 계약 terminate, SCHEDULED WorkRecord DELETED 처리")
    void withdrawEmployer_success() {
        // given
        Employer employerEntity = Employer.builder()
                .id(10L)
                .user(employer)
                .phone("010-1234-5678")
                .build();

        Workplace workplace = Workplace.builder()
                .id(100L)
                .employer(employerEntity)
                .name("테스트 사업장")
                .build();

        WorkerContract contract = WorkerContract.builder()
                .id(1000L)
                .workplace(workplace)
                .isActive(true)
                .build();

        when(employerRepository.findByUserId(employer.getId())).thenReturn(Optional.of(employerEntity));
        when(workplaceRepository.findByEmployerId(employerEntity.getId())).thenReturn(List.of(workplace));
        when(workerContractRepository.findByWorkplaceIdAndIsActive(workplace.getId(), true))
                .thenReturn(List.of(contract));
        when(workRecordRepository.bulkUpdateStatusByContractIdAndStatus(
                eq(contract.getId()), eq(WorkRecordStatus.SCHEDULED), eq(WorkRecordStatus.DELETED)))
                .thenReturn(3);
        when(userRepository.findById(employer.getId())).thenReturn(Optional.of(employer));

        // when
        userWithdrawService.withdraw(employer);

        // then
        assertThat(employer.isDeleted()).isTrue();
        assertThat(workplace.getIsActive()).isFalse();
        assertThat(contract.getIsActive()).isFalse();

        verify(workRecordRepository).bulkUpdateStatusByContractIdAndStatus(
                contract.getId(), WorkRecordStatus.SCHEDULED, WorkRecordStatus.DELETED);
        verify(refreshTokenRepository).deleteByUserId(employer.getId());
        verify(fcmTokenRepository).deleteByUserId(employer.getId());
        verify(notificationRepository).deleteAllByUser(employer);
    }

    @Test
    @DisplayName("근로자 탈퇴 성공 - 계약 terminate, SCHEDULED WorkRecord DELETED 처리")
    void withdrawWorker_success() {
        // given
        Worker workerEntity = Worker.builder()
                .id(20L)
                .user(worker)
                .workerCode("ABC123")
                .bankName("카카오뱅크")
                .accountNumber("1234567890")
                .build();

        WorkerContract activeContract = WorkerContract.builder()
                .id(2000L)
                .isActive(true)
                .build();

        WorkerContract inactiveContract = WorkerContract.builder()
                .id(2001L)
                .isActive(false)
                .build();

        when(workerRepository.findByUserId(worker.getId())).thenReturn(Optional.of(workerEntity));
        when(workerContractRepository.findByWorkerId(workerEntity.getId()))
                .thenReturn(List.of(activeContract, inactiveContract));
        when(userRepository.findById(worker.getId())).thenReturn(Optional.of(worker));

        // when
        userWithdrawService.withdraw(worker);

        // then
        assertThat(worker.isDeleted()).isTrue();
        assertThat(activeContract.getIsActive()).isFalse();

        // 활성 계약만 SCHEDULED→DELETED 벌크 업데이트
        verify(workRecordRepository).bulkUpdateStatusByContractIdAndStatus(
                activeContract.getId(), WorkRecordStatus.SCHEDULED, WorkRecordStatus.DELETED);
        // 비활성 계약은 처리하지 않음
        verify(workRecordRepository, never()).bulkUpdateStatusByContractIdAndStatus(
                eq(inactiveContract.getId()), any(), any());
    }

    @Test
    @DisplayName("이미 탈퇴한 사용자 재탈퇴 시 예외 발생")
    void withdraw_alreadyDeleted_throwsException() {
        // given
        User deletedEmployer = User.builder()
                .id(1L)
                .kakaoId("kakao_employer")
                .name("고용주")
                .userType(UserType.EMPLOYER)
                .build();
        deletedEmployer.withdraw();
        when(userRepository.findById(employer.getId())).thenReturn(Optional.of(deletedEmployer));

        // when & then
        assertThatThrownBy(() -> userWithdrawService.withdraw(employer))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("탈퇴 시 토큰/알림 정리, UserSettings는 보존 (30일 hard delete 시점에 정리됨)")
    void withdraw_cleanupsCommonData() {
        // given
        when(workerRepository.findByUserId(worker.getId())).thenReturn(Optional.empty());
        when(userRepository.findById(worker.getId())).thenReturn(Optional.of(worker));

        // when
        userWithdrawService.withdraw(worker);

        // then
        verify(refreshTokenRepository).deleteByUserId(worker.getId());
        verify(fcmTokenRepository).deleteByUserId(worker.getId());
        verify(notificationRepository).deleteAllByUser(worker);
        // UserSettings는 탈퇴 시점에 보존 (복구 시 사용자 알림 설정 유지)
    }

    @Test
    @DisplayName("Employer/Worker 프로필 없는 사용자 탈퇴 시 NPE 없음")
    void withdraw_noProfile_noException() {
        // given
        when(employerRepository.findByUserId(employer.getId())).thenReturn(Optional.empty());
        when(userRepository.findById(employer.getId())).thenReturn(Optional.of(employer));

        // when
        userWithdrawService.withdraw(employer);

        // then
        assertThat(employer.isDeleted()).isTrue();
        verify(workplaceRepository, never()).findByEmployerId(any());
    }

    @Test
    @DisplayName("COMPLETED WorkRecord는 벌크 업데이트에 포함되지 않음 (SCHEDULED만 대상)")
    void withdraw_onlyScheduledWorkRecordsDeleted() {
        // given
        Worker workerEntity = Worker.builder()
                .id(20L)
                .user(worker)
                .workerCode("ABC123")
                .bankName("카카오뱅크")
                .accountNumber("1234567890")
                .build();

        WorkerContract contract = WorkerContract.builder()
                .id(2000L)
                .isActive(true)
                .build();

        when(workerRepository.findByUserId(worker.getId())).thenReturn(Optional.of(workerEntity));
        when(workerContractRepository.findByWorkerId(workerEntity.getId())).thenReturn(List.of(contract));
        when(userRepository.findById(worker.getId())).thenReturn(Optional.of(worker));

        // when
        userWithdrawService.withdraw(worker);

        // then - SCHEDULED만 대상으로 벌크 업데이트 호출 확인
        verify(workRecordRepository).bulkUpdateStatusByContractIdAndStatus(
                contract.getId(), WorkRecordStatus.SCHEDULED, WorkRecordStatus.DELETED);
        // COMPLETED 상태로의 업데이트는 호출되지 않음
        verify(workRecordRepository, never()).bulkUpdateStatusByContractIdAndStatus(
                any(), eq(WorkRecordStatus.COMPLETED), any());
    }

    @Test
    @DisplayName("findById 결과가 비어있을 때 NotFoundException 발생")
    void withdraw_userNotFound_throwsNotFoundException() {
        // given
        when(userRepository.findById(employer.getId())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userWithdrawService.withdraw(employer))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }
}
