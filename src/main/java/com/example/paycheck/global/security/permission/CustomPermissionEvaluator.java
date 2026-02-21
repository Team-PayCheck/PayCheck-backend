package com.example.paycheck.global.security.permission;

import com.example.paycheck.domain.contract.entity.WorkerContract;
import com.example.paycheck.domain.contract.repository.WorkerContractRepository;
import com.example.paycheck.domain.correction.entity.CorrectionRequest;
import com.example.paycheck.domain.correction.repository.CorrectionRequestRepository;
import com.example.paycheck.domain.notice.entity.Notice;
import com.example.paycheck.domain.notice.repository.NoticeRepository;
import com.example.paycheck.domain.payment.entity.Payment;
import com.example.paycheck.domain.payment.repository.PaymentRepository;
import com.example.paycheck.domain.salary.entity.Salary;
import com.example.paycheck.domain.salary.repository.SalaryRepository;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.user.enums.UserType;
import com.example.paycheck.domain.worker.entity.Worker;
import com.example.paycheck.domain.worker.repository.WorkerRepository;
import com.example.paycheck.domain.workplace.entity.Workplace;
import com.example.paycheck.domain.workplace.repository.WorkplaceRepository;
import com.example.paycheck.domain.workrecord.entity.WorkRecord;
import com.example.paycheck.domain.workrecord.repository.WorkRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("permissionEvaluator")
@RequiredArgsConstructor
public class CustomPermissionEvaluator {

    private final WorkerContractRepository contractRepository;
    private final WorkplaceRepository workplaceRepository;
    private final WorkRecordRepository workRecordRepository;
    private final SalaryRepository salaryRepository;
    private final PaymentRepository paymentRepository;
    private final CorrectionRequestRepository correctionRequestRepository;
    private final WorkerRepository workerRepository;
    private final NoticeRepository noticeRepository;

    // ==================== 공통 유틸리티 ====================

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            return null;
        }
        return (User) authentication.getPrincipal();
    }

    // ==================== 역할 검증 ====================

    public boolean isEmployer() {
        User user = getCurrentUser();
        return user != null && UserType.EMPLOYER.equals(user.getUserType());
    }

    public boolean isWorker() {
        User user = getCurrentUser();
        return user != null && UserType.WORKER.equals(user.getUserType());
    }

    // ==================== USER 권한 ====================

    public boolean canAccessUser(Long userId) {
        User user = getCurrentUser();
        return user != null && user.getId().equals(userId);
    }

    // ==================== WORKER 권한 ====================

    public boolean canAccessWorker(Long workerId) {
        User user = getCurrentUser();
        if (user == null) {
            return false;
        }
        Worker worker = workerRepository.findById(workerId).orElse(null);
        return worker != null && worker.getUser().getId().equals(user.getId());
    }

    public boolean canAccessWorkerByUserId(Long userId) {
        User user = getCurrentUser();
        return user != null && user.getId().equals(userId);
    }

    // ==================== WORKPLACE 권한 ====================

    public boolean canAccessWorkplaceAsMember(Long workplaceId) {
        User user = getCurrentUser();
        if (user == null) {
            return false;
        }
        Workplace workplace = workplaceRepository.findById(workplaceId).orElse(null);
        if (workplace == null) {
            return false;
        }
        if (UserType.EMPLOYER.equals(user.getUserType())) {
            return workplace.getEmployer().getUser().getId().equals(user.getId());
        }
        if (UserType.WORKER.equals(user.getUserType())) {
            Worker worker = workerRepository.findByUserId(user.getId()).orElse(null);
            if (worker == null) {
                return false;
            }
            return contractRepository.findByWorkerIdAndWorkplaceId(worker.getId(), workplaceId)
                    .map(WorkerContract::getIsActive)
                    .orElse(false);
        }
        return false;
    }

    public boolean canAccessWorkplace(Long workplaceId) {
        User user = getCurrentUser();
        if (user == null) {
            return false;
        }
        Workplace workplace = workplaceRepository.findById(workplaceId).orElse(null);
        return workplace != null && workplace.getEmployer().getUser().getId().equals(user.getId());
    }

    // ==================== CONTRACT 권한 ====================

    public boolean canAccessContractAsEmployer(Long contractId) {
        User user = getCurrentUser();
        if (user == null) {
            return false;
        }
        WorkerContract contract = contractRepository.findById(contractId).orElse(null);
        return contract != null && contract.getWorkplace().getEmployer().getUser().getId().equals(user.getId());
    }

    public boolean canAccessContractAsWorker(Long contractId) {
        User user = getCurrentUser();
        if (user == null) {
            return false;
        }
        WorkerContract contract = contractRepository.findById(contractId).orElse(null);
        return contract != null && contract.getWorker().getUser().getId().equals(user.getId());
    }

    // ==================== WORK_RECORD 권한 ====================

    public boolean canAccessWorkRecordAsEmployer(Long workRecordId) {
        User user = getCurrentUser();
        if (user == null) {
            return false;
        }
        WorkRecord workRecord = workRecordRepository.findById(workRecordId).orElse(null);
        return workRecord != null && workRecord.getContract().getWorkplace()
                .getEmployer().getUser().getId().equals(user.getId());
    }

    public boolean canAccessWorkRecordAsWorker(Long workRecordId) {
        User user = getCurrentUser();
        if (user == null) {
            return false;
        }
        WorkRecord workRecord = workRecordRepository.findById(workRecordId).orElse(null);
        return workRecord != null && workRecord.getContract().getWorker()
                .getUser().getId().equals(user.getId());
    }

    public boolean canAccessWorkplaceRecords(Long workplaceId) {
        return canAccessWorkplace(workplaceId);
    }

    // ==================== SALARY 권한 ====================

    public boolean canAccessSalary(Long salaryId) {
        User user = getCurrentUser();
        if (user == null) {
            return false;
        }
        Salary salary = salaryRepository.findById(salaryId).orElse(null);
        return salary != null && salary.getContract().getWorkplace()
                .getEmployer().getUser().getId().equals(user.getId());
    }

    public boolean canAccessSalaryAsWorker(Long salaryId) {
        User user = getCurrentUser();
        if (user == null) {
            return false;
        }
        Salary salary = salaryRepository.findById(salaryId).orElse(null);
        return salary != null && salary.getContract().getWorker()
                .getUser().getId().equals(user.getId());
    }

    public boolean canAccessWorkplaceSalaries(Long workplaceId) {
        return canAccessWorkplace(workplaceId);
    }

    public boolean canCalculateSalaryForContract(Long contractId) {
        User user = getCurrentUser();
        if (user == null) {
            return false;
        }
        WorkerContract contract = contractRepository.findById(contractId).orElse(null);
        if (contract == null) {
            return false;
        }
        return contract.getWorkplace().getEmployer().getUser().getId().equals(user.getId())
                || contract.getWorker().getUser().getId().equals(user.getId());
    }

    // ==================== PAYMENT 권한 ====================

    public boolean canAccessPayment(Long paymentId) {
        User user = getCurrentUser();
        if (user == null) {
            return false;
        }
        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        return payment != null && payment.getSalary().getContract().getWorkplace()
                .getEmployer().getUser().getId().equals(user.getId());
    }

    public boolean canAccessWorkplacePayments(Long workplaceId) {
        return canAccessWorkplace(workplaceId);
    }

    // ==================== CORRECTION_REQUEST 권한 ====================

    public boolean canAccessCorrectionRequestAsEmployer(Long correctionRequestId) {
        User user = getCurrentUser();
        if (user == null) {
            return false;
        }
        CorrectionRequest request = correctionRequestRepository.findById(correctionRequestId).orElse(null);
        return request != null && request.getWorkRecord().getContract().getWorkplace()
                .getEmployer().getUser().getId().equals(user.getId());
    }

    public boolean canAccessCorrectionRequestAsWorker(Long correctionRequestId) {
        User user = getCurrentUser();
        if (user == null) {
            return false;
        }
        CorrectionRequest request = correctionRequestRepository.findById(correctionRequestId).orElse(null);
        return request != null && request.getRequester().getId().equals(user.getId());
    }

    public boolean canAccessWorkplaceCorrectionRequests(Long workplaceId) {
        return canAccessWorkplace(workplaceId);
    }

    // ==================== NOTICE 권한 ====================

    public boolean canAccessNotice(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId).orElse(null);
        if (notice == null) {
            return false;
        }
        return canAccessWorkplaceAsMember(notice.getWorkplace().getId());
    }
}
