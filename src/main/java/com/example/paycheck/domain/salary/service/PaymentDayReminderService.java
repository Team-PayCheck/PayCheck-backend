package com.example.paycheck.domain.salary.service;

import com.example.paycheck.domain.contract.entity.WorkerContract;
import com.example.paycheck.domain.contract.repository.WorkerContractRepository;
import com.example.paycheck.domain.notification.enums.NotificationActionType;
import com.example.paycheck.domain.notification.enums.NotificationType;
import com.example.paycheck.domain.notification.event.NotificationEvent;
import com.example.paycheck.domain.workrecord.entity.WorkRecord;
import com.example.paycheck.domain.workrecord.enums.WorkRecordStatus;
import com.example.paycheck.domain.workrecord.repository.WorkRecordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentDayReminderService {

    private final WorkerContractRepository workerContractRepository;
    private final WorkRecordRepository workRecordRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    /**
     * 급여 지급일 전날 근무 기록 확인 알림을 대상 근로자에게 발송합니다.
     *
     * 스케줄러(PaymentDayReminderScheduler)에서 매일 오전 9시에 호출됩니다.
     * 내일이 급여 지급일인 활성 계약의 근로자에게 알림을 발송합니다.
     */
    @Transactional
    public void sendPaymentDayReminders() {
        sendPaymentDayRemindersForDate(LocalDate.now());
    }

    /**
     * 주어진 기준 날짜로 급여 지급일 전날 알림을 처리합니다.
     * 테스트에서 날짜를 고정하여 호출할 수 있도록 패키지 프라이빗으로 분리합니다.
     */
    @Transactional
    void sendPaymentDayRemindersForDate(LocalDate today) {
        LocalDate tomorrow = today.plusDays(1);
        int tomorrowDay = tomorrow.getDayOfMonth();
        boolean isLastDayOfMonth = (tomorrowDay == tomorrow.lengthOfMonth());

        log.info("급여 지급일 전날 알림 처리 시작: 내일={}, 월말여부={}", tomorrow, isLastDayOfMonth);

        List<WorkerContract> contracts = isLastDayOfMonth
                ? workerContractRepository.findActiveContractsByPaymentDayOnLastDay(tomorrowDay)
                : workerContractRepository.findActiveContractsByExactPaymentDay(tomorrowDay);

        log.info("알림 대상 계약 수: {}건", contracts.size());

        int successCount = 0;
        int failCount = 0;

        for (WorkerContract contract : contracts) {
            try {
                sendReminderForContract(contract, tomorrow);
                successCount++;
            } catch (Exception e) {
                log.error("계약 ID={} 알림 발송 실패: {}", contract.getId(), e.getMessage(), e);
                failCount++;
            }
        }

        log.info("급여 지급일 전날 알림 처리 완료: 성공={}, 실패={}", successCount, failCount);
    }

    /**
     * 특정 계약에 대해 근무 기록 요약을 계산하고 알림 이벤트를 발행합니다.
     */
    private void sendReminderForContract(WorkerContract contract, LocalDate tomorrow) {
        int paymentDay = contract.getPaymentDay();
        int year = tomorrow.getYear();
        int month = tomorrow.getMonthValue();

        // SalaryService와 동일한 급여 기간 계산 로직
        LocalDate periodStart = adjustDayOfMonth(
                LocalDate.of(year, month, 1).minusMonths(1), paymentDay);
        LocalDate periodEnd = adjustDayOfMonth(
                LocalDate.of(year, month, 1), paymentDay).minusDays(1);

        // COMPLETED 근무 기록만 집계 (SCHEDULED/DELETED 제외)
        List<WorkRecord> records = workRecordRepository.findByContractAndDateRangeAndStatus(
                contract.getId(), periodStart, periodEnd, List.of(WorkRecordStatus.COMPLETED));

        int workDays = records.size();
        BigDecimal totalHours = records.stream()
                .map(WorkRecord::getTotalHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(1, RoundingMode.HALF_UP);

        String title = String.format(
                "내일은 %s 급여일이에요. %d년 %d월 근무 기록을 확인해 보세요! (근무 %d일, 총 %.1f시간)",
                contract.getWorkplace().getName(), year, month, workDays, totalHours);

        // Salary 레코드가 아직 없을 수 있으므로 contractId + year + month 사용
        String actionData = buildActionData(contract.getId(), year, month);

        NotificationEvent event = NotificationEvent.builder()
                .user(contract.getWorker().getUser())
                .type(NotificationType.WORK_RECORD_CONFIRMATION)
                .title(title)
                .actionType(NotificationActionType.VIEW_SALARY)
                .actionData(actionData)
                .build();

        eventPublisher.publishEvent(event);

        log.debug("알림 이벤트 발행: contractId={}, workerId={}, workDays={}, totalHours={}",
                contract.getId(), contract.getWorker().getId(), workDays, totalHours);
    }

    /**
     * VIEW_SALARY 액션 데이터를 생성합니다.
     * Salary 레코드가 아직 없을 수 있으므로 contractId + year + month 조합을 사용합니다.
     */
    private String buildActionData(Long contractId, int year, int month) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("contractId", contractId);
            data.put("year", year);
            data.put("month", month);
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.warn("알림 액션 데이터 생성 실패: contractId={}", contractId, e);
            return null;
        }
    }

    /**
     * SalaryService.adjustDayOfMonth()와 동일한 로직.
     * paymentDay가 해당 월의 일수를 초과하는 경우 월말 날짜로 조정합니다.
     */
    private LocalDate adjustDayOfMonth(LocalDate baseDate, int desiredDay) {
        int lastDay = baseDate.lengthOfMonth();
        return baseDate.withDayOfMonth(Math.max(1, Math.min(desiredDay, lastDay)));
    }
}
