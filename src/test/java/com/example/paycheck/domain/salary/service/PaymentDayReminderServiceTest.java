package com.example.paycheck.domain.salary.service;

import com.example.paycheck.domain.contract.entity.WorkerContract;
import com.example.paycheck.domain.contract.repository.WorkerContractRepository;
import com.example.paycheck.domain.notification.enums.NotificationActionType;
import com.example.paycheck.domain.notification.enums.NotificationType;
import com.example.paycheck.domain.notification.event.NotificationEvent;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.worker.entity.Worker;
import com.example.paycheck.domain.workrecord.entity.WorkRecord;
import com.example.paycheck.domain.workrecord.enums.WorkRecordStatus;
import com.example.paycheck.domain.workrecord.repository.WorkRecordRepository;
import com.example.paycheck.domain.workplace.entity.Workplace;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // @BeforeEach 공통 스텁이 일부 테스트에서 사용 안 될 수 있음
@DisplayName("PaymentDayReminderService 테스트")
@SuppressWarnings("null") // Mockito ArgumentCaptor/any()의 Eclipse null analysis 경고 억제
class PaymentDayReminderServiceTest {

    @Mock
    private WorkerContractRepository workerContractRepository;

    @Mock
    private WorkRecordRepository workRecordRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PaymentDayReminderService paymentDayReminderService;

    private User workerUser;
    private Worker worker;
    private Workplace workplace;
    private WorkerContract contract;

    @BeforeEach
    void setUp() {
        workerUser = mock(User.class);
        when(workerUser.getId()).thenReturn(100L);

        worker = mock(Worker.class);
        when(worker.getId()).thenReturn(10L);
        when(worker.getUser()).thenReturn(workerUser);

        workplace = mock(Workplace.class);
        when(workplace.getName()).thenReturn("테스트 카페");

        contract = mock(WorkerContract.class);
        when(contract.getId()).thenReturn(1L);
        when(contract.getWorker()).thenReturn(worker);
        when(contract.getWorkplace()).thenReturn(workplace);
    }

    @Test
    @DisplayName("일반 날짜에 paymentDay가 내일과 일치하는 계약 근로자에게 알림을 발송한다")
    void sendPaymentDayRemindersForDate_NormalDate_SendsNotification() throws Exception {
        // given
        // 오늘: 2025-03-20, 내일: 2025-03-21, 내일은 월말 아님
        LocalDate today = LocalDate.of(2025, 3, 20);
        when(contract.getPaymentDay()).thenReturn(21);

        WorkRecord workRecord = mock(WorkRecord.class);
        when(workRecord.getTotalHours()).thenReturn(new BigDecimal("8.0"));

        when(workerContractRepository.findActiveContractsByExactPaymentDay(21))
                .thenReturn(List.of(contract));
        when(workRecordRepository.findByContractAndDateRangeAndStatus(
                eq(1L),
                eq(LocalDate.of(2025, 2, 21)),   // periodStart: 전달 21일
                eq(LocalDate.of(2025, 3, 20)),   // periodEnd: 이달 20일
                eq(List.of(WorkRecordStatus.COMPLETED))))
                .thenReturn(List.of(workRecord));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"contractId\":1,\"year\":2025,\"month\":3}");

        // when
        paymentDayReminderService.sendPaymentDayRemindersForDate(today);

        // then
        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        NotificationEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getType()).isEqualTo(NotificationType.WORK_RECORD_CONFIRMATION);
        assertThat(capturedEvent.getActionType()).isEqualTo(NotificationActionType.VIEW_SALARY);
        assertThat(capturedEvent.getUser()).isEqualTo(workerUser);
        assertThat(capturedEvent.getTitle()).contains("테스트 카페");
        assertThat(capturedEvent.getTitle()).contains("2025년 3월");
        assertThat(capturedEvent.getTitle()).contains("근무 1일");
        assertThat(capturedEvent.getTitle()).contains("총 8.0시간");
    }

    @Test
    @DisplayName("월말 날짜에는 findActiveContractsByPaymentDayOnLastDay 쿼리를 사용한다")
    void sendPaymentDayRemindersForDate_LastDayOfMonth_UsesLastDayQuery() throws Exception {
        // given
        // 오늘: 2025-02-27, 내일: 2025-02-28 (2월 말일)
        LocalDate today = LocalDate.of(2025, 2, 27);
        when(contract.getPaymentDay()).thenReturn(31); // paymentDay=31 → 2월에는 28일로 조정

        when(workerContractRepository.findActiveContractsByPaymentDayOnLastDay(28))
                .thenReturn(List.of(contract));
        when(workRecordRepository.findByContractAndDateRangeAndStatus(
                eq(1L), any(LocalDate.class), any(LocalDate.class),
                eq(List.of(WorkRecordStatus.COMPLETED))))
                .thenReturn(Collections.emptyList());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // when
        paymentDayReminderService.sendPaymentDayRemindersForDate(today);

        // then
        verify(workerContractRepository).findActiveContractsByPaymentDayOnLastDay(28);
        verify(workerContractRepository, never()).findActiveContractsByExactPaymentDay(anyInt());
        verify(eventPublisher).publishEvent(any(NotificationEvent.class));
    }

    @Test
    @DisplayName("paymentDay=31인 계약이 2월 말일 전날에 올바른 급여 기간으로 알림을 발송한다")
    void sendPaymentDayRemindersForDate_PaymentDay31_InFebruary_CorrectPeriod() throws Exception {
        // given
        // 오늘: 2025-02-27, 내일: 2025-02-28 (2월 말일)
        LocalDate today = LocalDate.of(2025, 2, 27);
        when(contract.getPaymentDay()).thenReturn(31);

        WorkRecord workRecord1 = mock(WorkRecord.class);
        when(workRecord1.getTotalHours()).thenReturn(new BigDecimal("8.0"));
        WorkRecord workRecord2 = mock(WorkRecord.class);
        when(workRecord2.getTotalHours()).thenReturn(new BigDecimal("6.5"));

        when(workerContractRepository.findActiveContractsByPaymentDayOnLastDay(28))
                .thenReturn(List.of(contract));
        // periodStart: adjustDayOfMonth(2025-01-01, 31) = 2025-01-31
        // periodEnd: adjustDayOfMonth(2025-02-01, 31).minusDays(1) = 2025-02-28 - 1 = 2025-02-27
        when(workRecordRepository.findByContractAndDateRangeAndStatus(
                eq(1L),
                eq(LocalDate.of(2025, 1, 31)),
                eq(LocalDate.of(2025, 2, 27)),
                eq(List.of(WorkRecordStatus.COMPLETED))))
                .thenReturn(List.of(workRecord1, workRecord2));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // when
        paymentDayReminderService.sendPaymentDayRemindersForDate(today);

        // then
        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        NotificationEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getTitle()).contains("근무 2일");
        assertThat(capturedEvent.getTitle()).contains("총 14.5시간");
    }

    @Test
    @DisplayName("근무 기록이 0건이어도 알림을 발송한다")
    void sendPaymentDayRemindersForDate_NoWorkRecords_StillSendsNotification() throws Exception {
        // given
        LocalDate today = LocalDate.of(2025, 3, 20);
        when(contract.getPaymentDay()).thenReturn(21);

        when(workerContractRepository.findActiveContractsByExactPaymentDay(21))
                .thenReturn(List.of(contract));
        when(workRecordRepository.findByContractAndDateRangeAndStatus(
                eq(1L), any(LocalDate.class), any(LocalDate.class),
                eq(List.of(WorkRecordStatus.COMPLETED))))
                .thenReturn(Collections.emptyList());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // when
        paymentDayReminderService.sendPaymentDayRemindersForDate(today);

        // then
        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        NotificationEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getTitle()).contains("근무 0일");
        assertThat(capturedEvent.getTitle()).contains("총 0.0시간");
    }

    @Test
    @DisplayName("대상 계약이 없으면 알림을 발송하지 않는다")
    void sendPaymentDayRemindersForDate_NoContracts_NoNotificationSent() {
        // given
        LocalDate today = LocalDate.of(2025, 3, 20);

        when(workerContractRepository.findActiveContractsByExactPaymentDay(21))
                .thenReturn(Collections.emptyList());

        // when
        paymentDayReminderService.sendPaymentDayRemindersForDate(today);

        // then
        verify(eventPublisher, never()).publishEvent(any());
        verify(workRecordRepository, never()).findByContractAndDateRangeAndStatus(
                anyLong(), any(), any(), any());
    }

    @Test
    @DisplayName("특정 계약에서 예외가 발생해도 나머지 계약의 알림은 정상 발송된다")
    void sendPaymentDayRemindersForDate_OneContractFails_OtherContractStillSendsNotification() throws Exception {
        // given
        LocalDate today = LocalDate.of(2025, 3, 20);

        // sendReminderForContract 실행 순서: getPaymentDay → getId → getWorkplace().getName() → getWorker().getUser()
        // getWorkplace()가 getWorker()보다 먼저 호출되므로 getWorkplace()에서 예외를 발생시킴
        WorkerContract failingContract = mock(WorkerContract.class);
        when(failingContract.getId()).thenReturn(2L);
        when(failingContract.getPaymentDay()).thenReturn(21);
        when(failingContract.getWorkplace()).thenThrow(new RuntimeException("의도적 예외"));

        WorkRecord workRecord = mock(WorkRecord.class);
        when(workRecord.getTotalHours()).thenReturn(new BigDecimal("8.0"));

        when(contract.getPaymentDay()).thenReturn(21);
        when(workerContractRepository.findActiveContractsByExactPaymentDay(21))
                .thenReturn(List.of(failingContract, contract));
        when(workRecordRepository.findByContractAndDateRangeAndStatus(
                eq(1L), any(LocalDate.class), any(LocalDate.class),
                eq(List.of(WorkRecordStatus.COMPLETED))))
                .thenReturn(List.of(workRecord));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // when
        paymentDayReminderService.sendPaymentDayRemindersForDate(today);

        // then: 예외 발생한 계약은 실패하고, 나머지 1건은 정상 발송
        verify(eventPublisher, times(1)).publishEvent(any(NotificationEvent.class));
    }

    @Test
    @DisplayName("알림 이벤트의 actionData에 contractId, year, month가 포함된다")
    void sendPaymentDayRemindersForDate_ActionDataContainsContractInfo() throws Exception {
        // given
        LocalDate today = LocalDate.of(2025, 3, 20);
        when(contract.getPaymentDay()).thenReturn(21);

        String expectedActionData = "{\"contractId\":1,\"year\":2025,\"month\":3}";
        when(workerContractRepository.findActiveContractsByExactPaymentDay(21))
                .thenReturn(List.of(contract));
        when(workRecordRepository.findByContractAndDateRangeAndStatus(
                eq(1L), any(LocalDate.class), any(LocalDate.class),
                eq(List.of(WorkRecordStatus.COMPLETED))))
                .thenReturn(Collections.emptyList());
        when(objectMapper.writeValueAsString(any())).thenReturn(expectedActionData);

        // when
        paymentDayReminderService.sendPaymentDayRemindersForDate(today);

        // then
        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        assertThat(eventCaptor.getValue().getActionData()).isEqualTo(expectedActionData);
    }
}
