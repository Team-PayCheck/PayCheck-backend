package com.example.paycheck.domain.user.service;

import com.example.paycheck.common.exception.NotFoundException;
import com.example.paycheck.domain.allowance.repository.WeeklyAllowanceRepository;
import com.example.paycheck.domain.auth.repository.RefreshTokenRepository;
import com.example.paycheck.domain.contract.repository.WorkerContractRepository;
import com.example.paycheck.domain.correction.repository.CorrectionRequestRepository;
import com.example.paycheck.domain.employer.entity.Employer;
import com.example.paycheck.domain.employer.repository.EmployerRepository;
import com.example.paycheck.domain.fcm.repository.FcmTokenRepository;
import com.example.paycheck.domain.notice.repository.NoticeRepository;
import com.example.paycheck.domain.notification.repository.NotificationRepository;
import com.example.paycheck.domain.payment.repository.PaymentRepository;
import com.example.paycheck.domain.salary.repository.SalaryRepository;
import com.example.paycheck.domain.settings.repository.UserSettingsRepository;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.user.enums.UserType;
import com.example.paycheck.domain.user.repository.UserRepository;
import com.example.paycheck.domain.worker.entity.Worker;
import com.example.paycheck.domain.worker.repository.WorkerRepository;
import com.example.paycheck.domain.workplace.entity.Workplace;
import com.example.paycheck.domain.workplace.repository.WorkplaceRepository;
import com.example.paycheck.domain.workrecord.repository.WorkRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserHardDeleteService 테스트")
class UserHardDeleteServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private EmployerRepository employerRepository;
    @Mock private WorkerRepository workerRepository;
    @Mock private WorkplaceRepository workplaceRepository;
    @Mock private WorkerContractRepository workerContractRepository;
    @Mock private WorkRecordRepository workRecordRepository;
    @Mock private WeeklyAllowanceRepository weeklyAllowanceRepository;
    @Mock private SalaryRepository salaryRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private CorrectionRequestRepository correctionRequestRepository;
    @Mock private NoticeRepository noticeRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private FcmTokenRepository fcmTokenRepository;
    @Mock private UserSettingsRepository userSettingsRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private UserHardDeleteService userHardDeleteService;

    private User employerUser;
    private User workerUser;

    @BeforeEach
    void setUp() {
        employerUser = User.builder()
                .id(1L)
                .kakaoId("kakao_employer")
                .name("고용주")
                .userType(UserType.EMPLOYER)
                .build();
        employerUser.withdraw();

        workerUser = User.builder()
                .id(2L)
                .kakaoId("kakao_worker")
                .name("근로자")
                .userType(UserType.WORKER)
                .build();
        workerUser.withdraw();
    }

    @Test
    @DisplayName("고용주 영구 삭제 - 사업장/계약/하위 모든 데이터 삭제 + FK 순서 보장")
    void hardDeleteEmployer_success() {
        // given
        Employer employer = Employer.builder().id(10L).user(employerUser).phone("010-1111-2222").build();
        Workplace workplace = Workplace.builder().id(100L).employer(employer).name("사업장A").build();

        when(userRepository.findById(employerUser.getId())).thenReturn(Optional.of(employerUser));
        when(employerRepository.findByUserId(employerUser.getId())).thenReturn(Optional.of(employer));
        when(workplaceRepository.findByEmployerId(employer.getId())).thenReturn(List.of(workplace));
        when(workerContractRepository.findIdsByWorkplaceIdIn(List.of(100L))).thenReturn(List.of(1000L, 1001L));
        when(salaryRepository.findIdsByContractIdIn(List.of(1000L, 1001L))).thenReturn(List.of(5000L));

        // when
        userHardDeleteService.hardDeleteUser(employerUser.getId());

        // then - FK 순서 검증
        InOrder inOrder = inOrder(
                correctionRequestRepository, paymentRepository, salaryRepository,
                workRecordRepository, weeklyAllowanceRepository, workerContractRepository,
                noticeRepository, workplaceRepository, employerRepository, userRepository);

        inOrder.verify(correctionRequestRepository).deleteAllByRequesterId(employerUser.getId());
        inOrder.verify(correctionRequestRepository).deleteAllByContractIdIn(List.of(1000L, 1001L));
        inOrder.verify(paymentRepository).deleteAllBySalaryIdIn(List.of(5000L));
        inOrder.verify(salaryRepository).deleteAllByContractIdIn(List.of(1000L, 1001L));
        inOrder.verify(workRecordRepository).deleteAllByContractIdIn(List.of(1000L, 1001L));
        inOrder.verify(weeklyAllowanceRepository).deleteAllByContractIdIn(List.of(1000L, 1001L));
        inOrder.verify(workerContractRepository).deleteAllByWorkplaceIdIn(List.of(100L));
        inOrder.verify(noticeRepository).deleteAllByWorkplaceIdIn(List.of(100L));
        inOrder.verify(noticeRepository).deleteAllByAuthorId(employerUser.getId());
        inOrder.verify(workplaceRepository).deleteAllByEmployerId(employer.getId());
        inOrder.verify(employerRepository).delete(employer);
        inOrder.verify(userRepository).delete(employerUser);
    }

    @Test
    @DisplayName("근로자 영구 삭제 - 본인 계약/하위 데이터만 삭제")
    void hardDeleteWorker_success() {
        // given
        Worker worker = Worker.builder().id(20L).user(workerUser).workerCode("ABC123").build();

        when(userRepository.findById(workerUser.getId())).thenReturn(Optional.of(workerUser));
        when(workerRepository.findByUserId(workerUser.getId())).thenReturn(Optional.of(worker));
        when(workerContractRepository.findIdsByWorkerId(worker.getId())).thenReturn(List.of(2000L));
        when(salaryRepository.findIdsByContractIdIn(List.of(2000L))).thenReturn(List.of());

        // when
        userHardDeleteService.hardDeleteUser(workerUser.getId());

        // then
        InOrder inOrder = inOrder(
                correctionRequestRepository, salaryRepository, workRecordRepository,
                weeklyAllowanceRepository, workerContractRepository, noticeRepository,
                workerRepository, userRepository);

        inOrder.verify(correctionRequestRepository).deleteAllByRequesterId(workerUser.getId());
        inOrder.verify(correctionRequestRepository).deleteAllByContractIdIn(List.of(2000L));
        inOrder.verify(salaryRepository).deleteAllByContractIdIn(List.of(2000L));
        inOrder.verify(workRecordRepository).deleteAllByContractIdIn(List.of(2000L));
        inOrder.verify(weeklyAllowanceRepository).deleteAllByContractIdIn(List.of(2000L));
        inOrder.verify(workerContractRepository).deleteAllByWorkerId(worker.getId());
        inOrder.verify(noticeRepository).deleteAllByAuthorId(workerUser.getId());
        inOrder.verify(workerRepository).delete(worker);
        inOrder.verify(userRepository).delete(workerUser);

        // salary IDs가 비어있으므로 Payment 삭제는 호출되지 않음
        verify(paymentRepository, never()).deleteAllBySalaryIdIn(any());
    }

    @Test
    @DisplayName("공통 데이터 정리 - Notification, FcmToken, UserSettings, RefreshToken 삭제")
    void hardDeleteUser_cleanupsCommonData() {
        // given
        when(userRepository.findById(workerUser.getId())).thenReturn(Optional.of(workerUser));
        when(workerRepository.findByUserId(workerUser.getId())).thenReturn(Optional.empty());

        // when
        userHardDeleteService.hardDeleteUser(workerUser.getId());

        // then
        verify(notificationRepository).deleteAllByUser(workerUser);
        verify(fcmTokenRepository).deleteByUserId(workerUser.getId());
        verify(userSettingsRepository).deleteByUserId(workerUser.getId());
        verify(refreshTokenRepository).deleteByUserId(workerUser.getId());
        verify(userRepository).delete(workerUser);
    }

    @Test
    @DisplayName("Employer 프로필이 없어도 NPE 없이 정상 정리")
    void hardDeleteEmployer_noProfile_noException() {
        // given
        when(userRepository.findById(employerUser.getId())).thenReturn(Optional.of(employerUser));
        when(employerRepository.findByUserId(employerUser.getId())).thenReturn(Optional.empty());

        // when
        userHardDeleteService.hardDeleteUser(employerUser.getId());

        // then
        verify(workplaceRepository, never()).findByEmployerId(any());
        verify(workerContractRepository, never()).findIdsByWorkplaceIdIn(any());
        verify(userRepository).delete(employerUser);
    }

    @Test
    @DisplayName("계약/사업장이 없는 고용주는 빈 IN절 쿼리를 호출하지 않음")
    void hardDeleteEmployer_noWorkplaces_skipsBulkQueries() {
        // given
        Employer employer = Employer.builder().id(10L).user(employerUser).build();
        when(userRepository.findById(employerUser.getId())).thenReturn(Optional.of(employerUser));
        when(employerRepository.findByUserId(employerUser.getId())).thenReturn(Optional.of(employer));
        when(workplaceRepository.findByEmployerId(employer.getId())).thenReturn(List.of());

        // when
        userHardDeleteService.hardDeleteUser(employerUser.getId());

        // then - 빈 workplaceIds는 in절 쿼리 미호출
        verify(workerContractRepository, never()).findIdsByWorkplaceIdIn(any());
        verify(workerContractRepository, never()).deleteAllByWorkplaceIdIn(any());
        verify(noticeRepository, never()).deleteAllByWorkplaceIdIn(any());
        verify(workplaceRepository, never()).deleteAllByEmployerId(any());
        // 본인 작성 공지는 여전히 정리
        verify(noticeRepository).deleteAllByAuthorId(employerUser.getId());
        // requester 정정요청은 항상 정리
        verify(correctionRequestRepository).deleteAllByRequesterId(employerUser.getId());
        // contract IDs 비어있으므로 contract 기반 정정요청 삭제는 미호출
        verify(correctionRequestRepository, never()).deleteAllByContractIdIn(any());
        verify(employerRepository).delete(employer);
        verify(userRepository).delete(employerUser);
    }

    @Test
    @DisplayName("사용자가 존재하지 않으면 NotFoundException")
    void hardDeleteUser_notFound_throwsException() {
        // given
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userHardDeleteService.hardDeleteUser(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }
}
