package com.example.wagemanager.domain.payment.scheduler;

import com.example.wagemanager.domain.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 만료된 결제 건을 주기적으로 자동 실패 처리하는 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentAutoFailScheduler {

    private final PaymentService paymentService;

    /**
     * 매일 자정에 지급 예정일이 지난 PENDING 상태의 결제를 자동으로 FAILED 처리
     * cron 표현식: 초(0) 분(0) 시(0) 일(*) 월(*) 요일(*)
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void autoFailExpiredPayments() {
        log.info("만료된 결제 건 자동 실패 처리 작업 시작");

        try {
            paymentService.autoFailExpiredPendingPayments();
            log.info("만료된 결제 건 자동 실패 처리 작업 완료");
        } catch (Exception e) {
            log.error("만료된 결제 건 자동 실패 처리 중 오류 발생", e);
        }
    }
}
