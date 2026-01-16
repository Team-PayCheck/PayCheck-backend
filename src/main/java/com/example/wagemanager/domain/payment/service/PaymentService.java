package com.example.wagemanager.domain.payment.service;

import com.example.wagemanager.common.exception.BadRequestException;
import com.example.wagemanager.common.exception.ErrorCode;
import com.example.wagemanager.common.exception.NotFoundException;
import com.example.wagemanager.domain.payment.dto.PaymentDto;
import com.example.wagemanager.domain.payment.entity.Payment;
import com.example.wagemanager.domain.payment.enums.PaymentStatus;
import com.example.wagemanager.domain.payment.repository.PaymentRepository;
import com.example.wagemanager.domain.salary.entity.Salary;
import com.example.wagemanager.domain.salary.repository.SalaryRepository;
import com.example.wagemanager.domain.payment.enums.PaymentMethod;
import com.example.wagemanager.domain.payment.util.TossLinkGenerator;
import com.example.wagemanager.domain.worker.entity.Worker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SalaryRepository salaryRepository;

    /**
     * 급여 지급 처리 (토스 딥링크 고정)
     * - 급여가 계산되었는지 확인
     * - Payment 레코드 생성 또는 기존 레코드 업데이트
     * - 상태를 COMPLETED로 변경
     * - 지급 일시 및 거래 ID 저장
     */
    @Transactional
    public PaymentDto.Response processPayment(PaymentDto.PaymentRequest request) {
        // 급여 정보 조회
        Salary salary = salaryRepository.findById(request.getSalaryId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.SALARY_NOT_FOUND, "급여 정보를 찾을 수 없습니다."));

        // 급여가 계산되었는지 확인
        if (salary.getNetPay() == null || salary.getNetPay().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BadRequestException(ErrorCode.SALARY_NOT_CALCULATED, "급여가 계산되지 않아 송금할 수 없습니다. 급여를 먼저 계산해주세요.");
        }

        // 기존 Payment 레코드 확인
        Payment payment = paymentRepository.findBySalaryId(salary.getId())
                .orElse(null);

        if (payment != null) {
            // 기존 Payment가 있으면 업데이트
            if (payment.getStatus() == PaymentStatus.COMPLETED) {
                throw new BadRequestException(ErrorCode.PAYMENT_ALREADY_COMPLETED, "이미 송금이 완료된 급여입니다.");
            }
        } else {
            // 새로운 Payment 생성 (토스 딥링크 고정)
            payment = Payment.builder()
                    .salary(salary)
                    .paymentMethod(PaymentMethod.TOSS_DEEP_LINK)
                    .status(PaymentStatus.PENDING)
                    .build();
        }

        // 급여 송금 완료 처리
        payment.complete(UUID.randomUUID().toString());
        paymentRepository.save(payment);

        String tossLink = buildTossLink(salary);

        return PaymentDto.Response.from(payment, tossLink);
    }

    /**
     * 급여 송금 조회
     */
    public PaymentDto.Response getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PAYMENT_NOT_FOUND, "송금 기록을 찾을 수 없습니다."));
        String tossLink = buildTossLink(payment.getSalary());
        return PaymentDto.Response.from(payment, tossLink);
    }

    /**
     * 사업장별 송금 목록 조회
     */
    public List<PaymentDto.ListResponse> getPaymentsByWorkplace(Long workplaceId) {
        return paymentRepository.findByWorkplaceId(workplaceId)
                .stream()
                .map(PaymentDto.ListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 사업장별 연월 송금 목록 조회
     */
    public List<PaymentDto.ListResponse> getPaymentsByWorkplaceAndYearMonth(Long workplaceId, Integer year, Integer month) {
        return paymentRepository.findByWorkplaceIdAndYearMonth(workplaceId, year, month)
                .stream()
                .map(PaymentDto.ListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 미송금자 목록 조회 (송금 예정일이 지났는데 송금되지 않은 사람)
     */
    public List<PaymentDto.ListResponse> getUnpaidPayments(Long workplaceId, Integer year, Integer month) {
        return paymentRepository.findByWorkplaceIdAndYearMonthAndStatusNot(
                        workplaceId, year, month, PaymentStatus.COMPLETED)
                .stream()
                .map(PaymentDto.ListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 사업장의 송금 대기 목록 조회
     */
    public List<PaymentDto.ListResponse> getPendingPaymentsByWorkplace(Long workplaceId) {
        return paymentRepository.findByStatusAndWorkplaceId(PaymentStatus.PENDING, workplaceId)
                .stream()
                .map(PaymentDto.ListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 근로자 본인의 송금 내역 조회
     */
    public List<PaymentDto.ListResponse> getPaymentsByWorker(Long userId) {
        return paymentRepository.findByWorkerUserId(userId)
                .stream()
                .map(PaymentDto.ListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 송금 기한 초과 자동 실패 처리 (예약 작업)
     * - 급여 지급 예정일 당일에 송금되지 않으면 다음날 자동으로 FAILED 처리
     * - 예약 작업(Scheduled Task)에서 매일 호출됨
     */
    @Transactional
    public void autoFailExpiredPendingPayments() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        // 모든 PENDING 상태의 송금 기록 조회
        List<Payment> pendingPayments = paymentRepository.findByStatus(PaymentStatus.PENDING);

        for (Payment payment : pendingPayments) {
            Salary salary = payment.getSalary();
            LocalDate paymentDueDate = salary.getPaymentDueDate();

            // 지급 예정일이 어제 이전이면 자동 실패 처리 (예정일 당일 미송금 → 다음날 실패)
            if (paymentDueDate != null && paymentDueDate.isBefore(yesterday.plusDays(1))) {
                payment.fail("급여 지급 예정일(" + paymentDueDate + ")을 초과하여 자동으로 실패 처리되었습니다.");
                paymentRepository.save(payment);
            }
        }
    }

    private String buildTossLink(Salary salary) {
        Worker worker = salary.getContract().getWorker();
        if (worker == null) {
            throw new NotFoundException(ErrorCode.WORKER_NOT_FOUND, "근로자 정보를 찾을 수 없습니다.");
        }

        if (salary.getNetPay() == null || salary.getNetPay().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BadRequestException(ErrorCode.SALARY_NOT_CALCULATED, "송금 금액이 유효하지 않아 토스 링크를 생성할 수 없습니다.");
        }

        String bankName = worker.getBankName();
        String accountNumber = worker.getAccountNumber();
        if (!StringUtils.hasText(bankName) || !StringUtils.hasText(accountNumber)) {
            throw new BadRequestException(
                    ErrorCode.WORKER_BANK_INFO_REQUIRED, "근로자 계좌 정보가 없어 토스 링크를 생성할 수 없습니다."
            );
        }

        return TossLinkGenerator.generateSupertossLink(bankName, accountNumber, salary.getNetPay());
    }

}
