package com.example.paycheck.domain.workrecord.service;

import com.example.paycheck.common.exception.BadRequestException;
import com.example.paycheck.common.exception.NotFoundException;
import com.example.paycheck.domain.allowance.entity.WeeklyAllowance;
import com.example.paycheck.domain.contract.entity.WorkerContract;
import com.example.paycheck.domain.contract.repository.WorkerContractRepository;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.workrecord.dto.WorkRecordDto;
import com.example.paycheck.domain.workrecord.entity.WorkRecord;
import com.example.paycheck.domain.workrecord.enums.WorkRecordStatus;
import com.example.paycheck.domain.workrecord.repository.WorkRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkRecordCommandService 테스트")
class WorkRecordCommandServiceTest {

    @Mock
    private WorkRecordRepository workRecordRepository;

    @Mock
    private WorkerContractRepository workerContractRepository;

    @Mock
    private WorkRecordCoordinatorService coordinatorService;

    @Mock
    private WorkRecordCalculationService calculationService;

    @Mock
    private WorkRecordGenerationService workRecordGenerationService;

    @Mock
    private com.example.paycheck.domain.correction.repository.CorrectionRequestRepository correctionRequestRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private WorkRecordCommandService workRecordCommandService;

    private WorkerContract testContract;
    private WorkRecord testWorkRecord;
    private WeeklyAllowance testWeeklyAllowance;

    @BeforeEach
    void setUp() {
        testWeeklyAllowance = mock(WeeklyAllowance.class);
    }

    @Test
    @DisplayName("고용주의 근무 일정 생성 성공 - 미래 날짜 (SCHEDULED)")
    void createWorkRecordByEmployer_Success_Future() {
        // given
        testContract = mock(WorkerContract.class);
        testWorkRecord = mock(WorkRecord.class);
        User worker = mock(User.class);
        com.example.paycheck.domain.worker.entity.Worker workerEntity = mock(com.example.paycheck.domain.worker.entity.Worker.class);

        WorkRecordDto.CreateRequest request = WorkRecordDto.CreateRequest.builder()
                .contractId(1L)
                .workDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .breakMinutes(60)
                .memo("테스트 메모")
                .build();

        when(workerContractRepository.findById(anyLong())).thenReturn(Optional.of(testContract));
        when(coordinatorService.getOrCreateWeeklyAllowance(any(), any())).thenReturn(testWeeklyAllowance);
        when(workRecordRepository.save(any(WorkRecord.class))).thenReturn(testWorkRecord);
        when(testWorkRecord.getId()).thenReturn(1L);
        when(testWorkRecord.getContract()).thenReturn(testContract);
        when(testContract.getWorker()).thenReturn(workerEntity);
        when(workerEntity.getUser()).thenReturn(worker);

        // when
        WorkRecordDto.Response result = workRecordCommandService.createWorkRecordByEmployer(request);

        // then
        assertThat(result).isNotNull();
        verify(workRecordRepository).save(any(WorkRecord.class));
    }

    @Test
    @DisplayName("고용주의 근무 일정 생성 실패 - 계약 없음")
    void createWorkRecordByEmployer_Fail_ContractNotFound() {
        // given
        WorkRecordDto.CreateRequest request = WorkRecordDto.CreateRequest.builder()
                .contractId(999L)
                .workDate(LocalDate.now())
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .build();

        when(workerContractRepository.findById(anyLong())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> workRecordCommandService.createWorkRecordByEmployer(request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("근무 기록 업데이트 실패 - 기록 없음")
    void updateWorkRecord_NotFound() {
        // given
        WorkRecordDto.UpdateRequest request = WorkRecordDto.UpdateRequest.builder()
                .startTime(LocalTime.of(10, 0))
                .build();

        when(workRecordRepository.findById(anyLong())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> workRecordCommandService.updateWorkRecord(1L, request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("근무 완료 처리 성공")
    void completeWorkRecord_Success() {
        // given
        testWorkRecord = mock(WorkRecord.class);
        when(workRecordRepository.findById(anyLong())).thenReturn(Optional.of(testWorkRecord));

        // when
        workRecordCommandService.completeWorkRecord(1L);

        // then
        verify(testWorkRecord).complete();
        verify(calculationService).calculateWorkRecordDetails(testWorkRecord);
        verify(calculationService).validateWorkRecordConsistency(testWorkRecord);
        verify(coordinatorService).handleWorkRecordCompletion(testWorkRecord);
    }

    @Test
    @DisplayName("근무 일정 일괄 생성 성공 - 최적화 버전")
    void createWorkRecordsBatch_Success() {
        // given
        testContract = mock(WorkerContract.class);
        User worker = mock(User.class);
        com.example.paycheck.domain.worker.entity.Worker workerEntity = mock(com.example.paycheck.domain.worker.entity.Worker.class);

        List<LocalDate> workDates = Arrays.asList(
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(2),
                LocalDate.now().plusDays(3)
        );

        WorkRecordDto.BatchCreateRequest request = WorkRecordDto.BatchCreateRequest.builder()
                .contractId(1L)
                .workDates(workDates)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .breakMinutes(60)
                .memo("일괄 생성 메모")
                .build();

        // 중복 체크 일괄 조회 Mock (중복 없음)
        when(workerContractRepository.findById(anyLong())).thenReturn(Optional.of(testContract));
        when(testContract.getId()).thenReturn(1L);
        when(workRecordRepository.findExistingWorkDatesByContractAndWorkDates(anyLong(), any(), any(WorkRecordStatus.class)))
                .thenReturn(Collections.emptyList());

        // WeeklyAllowance 일괄 조회/생성 Mock
        Map<LocalDate, WeeklyAllowance> weeklyAllowanceMap = new HashMap<>();
        for (LocalDate date : workDates) {
            LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            weeklyAllowanceMap.putIfAbsent(weekStart, testWeeklyAllowance);
        }
        when(coordinatorService.getOrCreateWeeklyAllowances(anyLong(), any()))
                .thenReturn(weeklyAllowanceMap);
        when(testWeeklyAllowance.getId()).thenReturn(1L);

        // saveAll Mock
        when(workRecordRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        when(testContract.getWorker()).thenReturn(workerEntity);
        when(workerEntity.getUser()).thenReturn(worker);

        // when
        WorkRecordDto.BatchCreateResponse result = workRecordCommandService.createWorkRecordsBatch(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getCreatedCount()).isEqualTo(3);
        assertThat(result.getSkippedCount()).isEqualTo(0);
        assertThat(result.getTotalRequested()).isEqualTo(3);

        // 검증: saveAll이 호출되었는지 (개별 save 대신)
        verify(workRecordRepository, atLeastOnce()).saveAll(anyList());
        verify(workRecordRepository, never()).save(any(WorkRecord.class));

        // 검증: 중복 체크 일괄 조회
        verify(workRecordRepository, times(1))
                .findExistingWorkDatesByContractAndWorkDates(anyLong(), any(), any(WorkRecordStatus.class));

        // 검증: WeeklyAllowance 일괄 조회/생성
        verify(coordinatorService, times(1))
                .getOrCreateWeeklyAllowances(anyLong(), any());

        // 검증: 도메인 협력 일괄 처리
        verify(coordinatorService, times(1))
                .handleBatchWorkRecordCreation(anyList());
    }

    @Test
    @DisplayName("근무 일정 일괄 생성 - 중복 스킵 (최적화 버전)")
    void createWorkRecordsBatch_WithDuplicates() {
        // given
        testContract = mock(WorkerContract.class);
        User worker = mock(User.class);
        com.example.paycheck.domain.worker.entity.Worker workerEntity = mock(com.example.paycheck.domain.worker.entity.Worker.class);

        LocalDate date1 = LocalDate.now().plusDays(1);
        LocalDate date2 = LocalDate.now().plusDays(2);

        WorkRecordDto.BatchCreateRequest request = WorkRecordDto.BatchCreateRequest.builder()
                .contractId(1L)
                .workDates(Arrays.asList(date1, date2))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .build();

        when(workerContractRepository.findById(anyLong())).thenReturn(Optional.of(testContract));
        when(testContract.getId()).thenReturn(1L);

        // 첫 번째 날짜는 이미 존재 (중복)
        when(workRecordRepository.findExistingWorkDatesByContractAndWorkDates(anyLong(), any(), any(WorkRecordStatus.class)))
                .thenReturn(Collections.singletonList(date1));

        // WeeklyAllowance Mock (date2만 생성됨)
        Map<LocalDate, WeeklyAllowance> weeklyAllowanceMap = new HashMap<>();
        LocalDate weekStart = date2.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        weeklyAllowanceMap.put(weekStart, testWeeklyAllowance);
        when(coordinatorService.getOrCreateWeeklyAllowances(anyLong(), any()))
                .thenReturn(weeklyAllowanceMap);
        when(testWeeklyAllowance.getId()).thenReturn(1L);

        when(workRecordRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(testContract.getWorker()).thenReturn(workerEntity);
        when(workerEntity.getUser()).thenReturn(worker);

        // when
        WorkRecordDto.BatchCreateResponse result = workRecordCommandService.createWorkRecordsBatch(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getCreatedCount()).isEqualTo(1);
        assertThat(result.getSkippedCount()).isEqualTo(1);

        // saveAll이 호출되고 개별 save는 호출되지 않음
        verify(workRecordRepository, atLeastOnce()).saveAll(anyList());
        verify(workRecordRepository, never()).save(any(WorkRecord.class));
    }
}
