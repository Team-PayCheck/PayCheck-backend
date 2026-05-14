package com.example.paycheck.domain.user.service;

import com.example.paycheck.common.exception.BadRequestException;
import com.example.paycheck.common.exception.ErrorCode;
import com.example.paycheck.common.exception.NotFoundException;
import com.example.paycheck.domain.allowance.repository.WeeklyAllowanceRepository;
import com.example.paycheck.domain.allowance.service.WeeklyAllowanceService;
import com.example.paycheck.domain.auth.repository.RefreshTokenRepository;
import com.example.paycheck.domain.contract.entity.WorkerContract;
import com.example.paycheck.domain.contract.repository.WorkerContractRepository;
import com.example.paycheck.domain.employer.entity.Employer;
import com.example.paycheck.domain.employer.repository.EmployerRepository;
import com.example.paycheck.domain.fcm.repository.FcmTokenRepository;
import com.example.paycheck.domain.notification.repository.NotificationRepository;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.user.enums.UserType;
import com.example.paycheck.domain.user.repository.UserRepository;
import com.example.paycheck.domain.worker.entity.Worker;
import com.example.paycheck.domain.worker.repository.WorkerRepository;
import com.example.paycheck.domain.workplace.entity.Workplace;
import com.example.paycheck.domain.workplace.repository.WorkplaceRepository;
import com.example.paycheck.domain.workrecord.enums.WorkRecordStatus;
import com.example.paycheck.domain.workrecord.repository.WorkRecordRepository;
import com.example.paycheck.domain.workrecord.service.WorkRecordCoordinatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserWithdrawService {

    private final EmployerRepository employerRepository;
    private final WorkerRepository workerRepository;
    private final WorkplaceRepository workplaceRepository;
    private final WorkerContractRepository workerContractRepository;
    private final WorkRecordRepository workRecordRepository;
    private final WeeklyAllowanceService weeklyAllowanceService;
    private final WeeklyAllowanceRepository weeklyAllowanceRepository;
    private final WorkRecordCoordinatorService workRecordCoordinatorService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * 회원 탈퇴 처리
     */
    public void withdraw(User user) {
        // managed 엔티티를 먼저 로드하여 DB 기준으로 검증/처리 수행
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        if (managedUser.isDeleted()) {
            throw new BadRequestException(ErrorCode.USER_ALREADY_DELETED, "이미 탈퇴한 사용자입니다.");
        }

        if (managedUser.getUserType() == UserType.EMPLOYER) {
            withdrawEmployer(managedUser);
        } else {
            withdrawWorker(managedUser);
        }

        cleanupCommonData(managedUser);
        managedUser.withdraw();

        log.info("회원 탈퇴 완료: userId={}, userType={}", managedUser.getId(), managedUser.getUserType());
    }

    /**
     * 고용주 탈퇴 처리
     * - 소유한 모든 Workplace 비활성화
     * - 각 Workplace의 활성 계약 terminate
     * - 각 계약의 SCHEDULED WorkRecord를 DELETED로 변경
     */
    private void withdrawEmployer(User user) {
        Employer employer = employerRepository.findByUserId(user.getId()).orElse(null);
        if (employer == null) {
            return;
        }

        List<Workplace> workplaces = workplaceRepository.findByEmployerId(employer.getId());
        for (Workplace workplace : workplaces) {
            workplace.deactivate();

            List<WorkerContract> contracts = workerContractRepository
                    .findByWorkplaceIdAndIsActive(workplace.getId(), true);
            for (WorkerContract contract : contracts) {
                contract.terminate();
                workRecordRepository.bulkUpdateStatusByContractIdAndStatus(
                        contract.getId(),
                        WorkRecordStatus.SCHEDULED,
                        WorkRecordStatus.DELETED);
                recalculateTerminationWeekAllowanceAndSalary(contract);
            }
        }
    }

    /**
     * 근로자 탈퇴 처리
     * - 모든 활성 계약 terminate
     * - 각 계약의 SCHEDULED WorkRecord를 DELETED로 변경
     */
    private void withdrawWorker(User user) {
        Worker worker = workerRepository.findByUserId(user.getId()).orElse(null);
        if (worker == null) {
            return;
        }

        List<WorkerContract> contracts = workerContractRepository.findByWorkerId(worker.getId());
        for (WorkerContract contract : contracts) {
            if (Boolean.TRUE.equals(contract.getIsActive())) {
                contract.terminate();
                workRecordRepository.bulkUpdateStatusByContractIdAndStatus(
                        contract.getId(),
                        WorkRecordStatus.SCHEDULED,
                        WorkRecordStatus.DELETED);
                recalculateTerminationWeekAllowanceAndSalary(contract);
            }
        }
    }

    /**
     * 계약 종료 주의 주휴수당 재계산 + 급여 재계산
     */
    private void recalculateTerminationWeekAllowanceAndSalary(WorkerContract contract) {
        LocalDate terminationDate = contract.getContractEndDate();
        weeklyAllowanceRepository.findByContractAndWeek(contract.getId(), terminationDate)
            .ifPresent(allowance -> {
                weeklyAllowanceService.recalculateAllowances(allowance.getId());
                workRecordCoordinatorService.recalculateSalaryForDate(
                    contract.getId(), contract.getPaymentDay(), terminationDate);
            });
    }

    /**
     * 고용주 계정 복구 시 탈퇴 과정에서 비활성화된 사업장 재활성화
     * Worker 타입이거나 Employer 프로필이 없으면 아무것도 하지 않는다.
     */
    public void restoreEmployerWorkplaces(User user) {
        if (user.getUserType() != UserType.EMPLOYER) {
            return;
        }
        Employer employer = employerRepository.findByUserId(user.getId()).orElse(null);
        if (employer == null) {
            return;
        }
        List<Workplace> deactivatedWorkplaces =
                workplaceRepository.findByEmployerIdAndIsActive(employer.getId(), false);
        deactivatedWorkplaces.forEach(Workplace::activate);
    }

    /**
     * 공통 데이터 정리
     * UserSettings는 30일 이내 복구 시 사용자가 이전에 설정한 알림 옵션을 그대로 살리기 위해
     * 탈퇴 시점에는 보존하며, 30일 후 hard delete 시 UserHardDeleteService에서 함께 정리된다.
     */
    private void cleanupCommonData(User user) {
        refreshTokenRepository.deleteByUserId(user.getId());
        fcmTokenRepository.deleteByUserId(user.getId());
        notificationRepository.deleteAllByUser(user);
    }
}
