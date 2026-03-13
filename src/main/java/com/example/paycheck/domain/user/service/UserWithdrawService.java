package com.example.paycheck.domain.user.service;

import com.example.paycheck.common.exception.BadRequestException;
import com.example.paycheck.common.exception.ErrorCode;
import com.example.paycheck.domain.allowance.repository.WeeklyAllowanceRepository;
import com.example.paycheck.domain.allowance.service.WeeklyAllowanceService;
import com.example.paycheck.domain.auth.repository.RefreshTokenRepository;
import com.example.paycheck.domain.contract.entity.WorkerContract;
import com.example.paycheck.domain.contract.repository.WorkerContractRepository;
import com.example.paycheck.domain.employer.entity.Employer;
import com.example.paycheck.domain.employer.repository.EmployerRepository;
import com.example.paycheck.domain.fcm.repository.FcmTokenRepository;
import com.example.paycheck.domain.notification.repository.NotificationRepository;
import com.example.paycheck.domain.settings.repository.UserSettingsRepository;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.user.enums.UserType;
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
    private final UserSettingsRepository userSettingsRepository;

    /**
     * 회원 탈퇴 처리
     */
    public void withdraw(User user) {
        if (user.isDeleted()) {
            throw new BadRequestException(ErrorCode.USER_ALREADY_DELETED, "이미 탈퇴한 사용자입니다.");
        }

        if (user.getUserType() == UserType.EMPLOYER) {
            withdrawEmployer(user);
        } else {
            withdrawWorker(user);
        }

        cleanupCommonData(user);
        user.withdraw();

        log.info("회원 탈퇴 완료: userId={}, userType={}", user.getId(), user.getUserType());
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
     * 공통 데이터 정리
     */
    private void cleanupCommonData(User user) {
        refreshTokenRepository.deleteByUserId(user.getId());
        fcmTokenRepository.deleteByUserId(user.getId());
        notificationRepository.deleteAllByUser(user);
        userSettingsRepository.deleteByUserId(user.getId());
    }
}
