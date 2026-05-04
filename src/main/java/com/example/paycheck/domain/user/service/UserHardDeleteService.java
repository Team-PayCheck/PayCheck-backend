package com.example.paycheck.domain.user.service;

import com.example.paycheck.common.exception.ErrorCode;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 탈퇴 30일 경과 사용자의 데이터를 영구 삭제하는 서비스.
 * 트랜잭션은 사용자 1명 단위로 분리하여, 한 명의 실패가 다른 사용자에게 전파되지 않도록 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserHardDeleteService {

    private final UserRepository userRepository;
    private final EmployerRepository employerRepository;
    private final WorkerRepository workerRepository;
    private final WorkplaceRepository workplaceRepository;
    private final WorkerContractRepository workerContractRepository;
    private final WorkRecordRepository workRecordRepository;
    private final WeeklyAllowanceRepository weeklyAllowanceRepository;
    private final SalaryRepository salaryRepository;
    private final PaymentRepository paymentRepository;
    private final CorrectionRequestRepository correctionRequestRepository;
    private final NoticeRepository noticeRepository;
    private final NotificationRepository notificationRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 단일 사용자 영구 삭제.
     * FK 제약을 고려한 순서로 연관 엔티티를 모두 hard delete 한다.
     */
    @Transactional
    public void hardDeleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        if (user.getUserType() == UserType.EMPLOYER) {
            hardDeleteEmployer(user);
        } else {
            hardDeleteWorker(user);
        }

        cleanupCommonData(user);
        userRepository.delete(user);

        log.info("사용자 영구 삭제 완료: userId={}, userType={}", user.getId(), user.getUserType());
    }

    /**
     * 고용주 영구 삭제: 사업장 및 그 산하 모든 계약/근무/급여/결제/정정요청/공지를 정리한다.
     */
    private void hardDeleteEmployer(User user) {
        Employer employer = employerRepository.findByUserId(user.getId()).orElse(null);
        if (employer == null) {
            return;
        }

        List<Workplace> workplaces = workplaceRepository.findByEmployerId(employer.getId());
        List<Long> workplaceIds = workplaces.stream().map(Workplace::getId).toList();

        List<Long> contractIds = workplaceIds.isEmpty()
                ? List.of()
                : workerContractRepository.findIdsByWorkplaceIdIn(workplaceIds);

        deleteContractDescendants(user.getId(), contractIds);

        if (!workplaceIds.isEmpty()) {
            workerContractRepository.deleteAllByWorkplaceIdIn(workplaceIds);
            noticeRepository.deleteAllByWorkplaceIdIn(workplaceIds);
        }
        // 사용자가 작성한 다른 사업장의 공지(가능성)도 정리
        noticeRepository.deleteAllByAuthorId(user.getId());

        if (!workplaceIds.isEmpty()) {
            workplaceRepository.deleteAllByEmployerId(employer.getId());
        }

        employerRepository.delete(employer);
    }

    /**
     * 근로자 영구 삭제: 본인의 모든 계약 및 그 산하 데이터를 정리한다.
     * 다른 근로자나 고용주의 데이터에는 영향을 주지 않는다.
     */
    private void hardDeleteWorker(User user) {
        Worker worker = workerRepository.findByUserId(user.getId()).orElse(null);
        if (worker == null) {
            return;
        }

        List<Long> contractIds = workerContractRepository.findIdsByWorkerId(worker.getId());

        deleteContractDescendants(user.getId(), contractIds);

        if (!contractIds.isEmpty()) {
            workerContractRepository.deleteAllByWorkerId(worker.getId());
        }
        // 근로자가 작성한 공지(가능성)도 정리
        noticeRepository.deleteAllByAuthorId(user.getId());

        workerRepository.delete(worker);
    }

    /**
     * 계약 산하 데이터를 FK 의존성 역순으로 삭제한다.
     *  Payment → Salary → WorkRecord → WeeklyAllowance → CorrectionRequest
     *  (CorrectionRequest는 WorkRecord/Contract를 참조하므로 가장 먼저 정리)
     */
    private void deleteContractDescendants(Long userId, List<Long> contractIds) {
        // 사용자가 요청한 모든 정정요청 (다른 사업장에서의 요청 포함) 정리
        correctionRequestRepository.deleteAllByRequesterId(userId);

        if (contractIds.isEmpty()) {
            return;
        }

        // 계약 직접 참조 + WorkRecord 경유 정정요청 정리
        correctionRequestRepository.deleteAllByContractIdIn(contractIds);

        // Payment는 Salary FK를 가지므로 먼저 삭제
        List<Long> salaryIds = salaryRepository.findIdsByContractIdIn(contractIds);
        if (!salaryIds.isEmpty()) {
            paymentRepository.deleteAllBySalaryIdIn(salaryIds);
        }
        salaryRepository.deleteAllByContractIdIn(contractIds);

        // WorkRecord는 weekly_allowance_id FK를 가지므로 WeeklyAllowance보다 먼저 삭제
        workRecordRepository.deleteAllByContractIdIn(contractIds);
        weeklyAllowanceRepository.deleteAllByContractIdIn(contractIds);
    }

    /**
     * 사용자 단위 공통 부속 데이터 정리.
     */
    private void cleanupCommonData(User user) {
        notificationRepository.deleteAllByUser(user);
        fcmTokenRepository.deleteByUserId(user.getId());
        userSettingsRepository.deleteByUserId(user.getId());
        refreshTokenRepository.deleteByUserId(user.getId());
    }
}
