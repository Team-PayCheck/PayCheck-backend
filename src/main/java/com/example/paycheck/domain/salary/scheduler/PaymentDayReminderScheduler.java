package com.example.paycheck.domain.salary.scheduler;

import com.example.paycheck.domain.salary.service.PaymentDayReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 급여 지급일 전날 근무 기록 확인 알림을 발송하는 스케줄러.
 *
 * 매일 오전 9시에 실행하여 내일이 급여 지급일인 활성 계약의 근로자에게 알림을 발송합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentDayReminderScheduler {

    private final PaymentDayReminderService paymentDayReminderService;

    /**
     * 매일 오전 9시에 실행합니다.
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendPaymentDayReminders() {
        log.info("급여 지급일 전날 알림 스케줄러 시작");
        try {
            paymentDayReminderService.sendPaymentDayReminders();
            log.info("급여 지급일 전날 알림 스케줄러 완료");
        } catch (Exception e) {
            log.error("급여 지급일 전날 알림 스케줄러 오류 발생", e);
        }
    }
}
