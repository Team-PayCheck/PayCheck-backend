package com.example.paycheck.domain.workrecord.service;

import com.example.paycheck.common.exception.NotFoundException;
import com.example.paycheck.domain.correction.repository.CorrectionRequestRepository;
import com.example.paycheck.domain.contract.entity.WorkerContract;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.worker.entity.Worker;
import com.example.paycheck.domain.worker.repository.WorkerRepository;
import com.example.paycheck.domain.workrecord.dto.WorkRecordDto;
import com.example.paycheck.domain.workrecord.entity.WorkRecord;
import com.example.paycheck.domain.workrecord.enums.WorkRecordCurrentStatus;
import com.example.paycheck.domain.workrecord.enums.WorkRecordStatus;
import com.example.paycheck.domain.workrecord.repository.WorkRecordRepository;
import com.example.paycheck.domain.workplace.entity.Workplace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkRecordQueryService 테스트")
class WorkRecordQueryServiceTest {

    @Mock
    private WorkRecordRepository workRecordRepository;

    @Mock
    private WorkerRepository workerRepository;

    @Mock
    private CorrectionRequestRepository correctionRequestRepository;

    @Spy
    private Clock clock = Clock.fixed(
            Instant.parse("2026-02-21T01:30:00Z"),
            ZoneId.of("UTC")
    );

    @InjectMocks
    private WorkRecordQueryService workRecordQueryService;


    @BeforeEach
    void setUp() {
        // Mock 데이터는 각 테스트에서 필요에 따라 설정
    }

    @Test
    @DisplayName("계약별 근무 기록 조회 성공")
    void getWorkRecordsByContract_Success() {
        // given
        Long contractId = 1L;
        when(workRecordRepository.findByContractId(contractId, WorkRecordStatus.DELETED)).thenReturn(Arrays.asList());

        // when
        List<WorkRecordDto.Response> result = workRecordQueryService.getWorkRecordsByContract(contractId);

        // then
        assertThat(result).isNotNull();
        verify(workRecordRepository).findByContractId(contractId, WorkRecordStatus.DELETED);
    }

    @Test
    @DisplayName("근무 기록 ID로 조회 실패 - 존재하지 않음")
    void getWorkRecordById_NotFound() {
        // given
        when(workRecordRepository.findById(anyLong())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> workRecordQueryService.getWorkRecordById(1L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("사업장 및 날짜 범위로 근무 기록 조회 성공")
    void getWorkRecordsByWorkplaceAndDateRange_Success() {
        // given
        Long workplaceId = 1L;
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        when(workRecordRepository.findByWorkplaceAndDateRange(workplaceId, startDate, endDate, WorkRecordStatus.DELETED))
                .thenReturn(Arrays.asList());

        // when
        List<WorkRecordDto.CalendarResponse> result = workRecordQueryService
                .getWorkRecordsByWorkplaceAndDateRange(workplaceId, startDate, endDate);

        // then
        assertThat(result).isNotNull();
        verify(workRecordRepository).findByWorkplaceAndDateRange(workplaceId, startDate, endDate, WorkRecordStatus.DELETED);
    }

    @Test
    @DisplayName("근로자 및 날짜 범위로 근무 기록 조회 실패 - 근로자 없음")
    void getWorkRecordsByWorkerAndDateRange_WorkerNotFound() {
        // given
        when(workerRepository.findByUserId(anyLong())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> workRecordQueryService.getWorkRecordsByWorkerAndDateRange(
                1L, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("근로자 주간 근무 목록 조회 시 현재 상태 기준으로 정렬한다")
    void getWorkRecordsByWorkerAndDateRange_SortsByCurrentStatus() {
        // given
        Worker worker = mock(Worker.class);
        when(worker.getId()).thenReturn(1L);
        when(workerRepository.findByUserId(10L)).thenReturn(Optional.of(worker));

        WorkRecord upcomingRecord = createWorkRecord(3L, LocalDate.of(2026, 2, 21), LocalTime.of(3, 0), LocalTime.of(7, 0), WorkRecordStatus.SCHEDULED);
        WorkRecord completedRecord = createWorkRecord(2L, LocalDate.of(2026, 2, 20), LocalTime.of(20, 0), LocalTime.of(23, 0), WorkRecordStatus.SCHEDULED);
        WorkRecord inProgressRecord = createWorkRecord(1L, LocalDate.of(2026, 2, 20), LocalTime.of(22, 0), LocalTime.of(2, 0), WorkRecordStatus.SCHEDULED);

        when(workRecordRepository.findByWorkerAndDateRange(
                1L, LocalDate.of(2026, 2, 17), LocalDate.of(2026, 2, 23), WorkRecordStatus.DELETED))
                .thenReturn(Arrays.asList(upcomingRecord, completedRecord, inProgressRecord));

        // when
        List<WorkRecordDto.DetailedResponse> result = workRecordQueryService.getWorkRecordsByWorkerAndDateRange(
                10L, LocalDate.of(2026, 2, 17), LocalDate.of(2026, 2, 23));

        // then
        assertThat(result).extracting(WorkRecordDto.DetailedResponse::getId)
                .containsExactly(1L, 3L, 2L);
        assertThat(result).extracting(WorkRecordDto.DetailedResponse::getCurrentStatus)
                .containsExactly(
                        WorkRecordCurrentStatus.IN_PROGRESS,
                        WorkRecordCurrentStatus.UPCOMING,
                        WorkRecordCurrentStatus.COMPLETED
                );
        assertThat(result).extracting(WorkRecordDto.DetailedResponse::getStatus)
                .containsExactly(
                        WorkRecordStatus.SCHEDULED,
                        WorkRecordStatus.SCHEDULED,
                        WorkRecordStatus.SCHEDULED
                );
    }

    @Test
    @DisplayName("완료 상태 근무는 현재 시점과 무관하게 완료로 유지한다")
    void getWorkRecordsByWorkerAndDateRange_KeepsCompletedStatus() {
        // given
        Worker worker = mock(Worker.class);
        when(worker.getId()).thenReturn(1L);
        when(workerRepository.findByUserId(10L)).thenReturn(Optional.of(worker));

        WorkRecord completedRecord = createWorkRecord(11L, LocalDate.of(2026, 2, 21), LocalTime.of(3, 0), LocalTime.of(7, 0), WorkRecordStatus.COMPLETED);

        when(workRecordRepository.findByWorkerAndDateRange(
                1L, LocalDate.of(2026, 2, 21), LocalDate.of(2026, 2, 21), WorkRecordStatus.DELETED))
                .thenReturn(List.of(completedRecord));

        // when
        List<WorkRecordDto.DetailedResponse> result = workRecordQueryService.getWorkRecordsByWorkerAndDateRange(
                10L, LocalDate.of(2026, 2, 21), LocalDate.of(2026, 2, 21));

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCurrentStatus()).isEqualTo(WorkRecordCurrentStatus.COMPLETED);
        assertThat(result.get(0).getStatus()).isEqualTo(WorkRecordStatus.COMPLETED);
    }

    private WorkRecord createWorkRecord(Long id, LocalDate workDate, LocalTime startTime, LocalTime endTime, WorkRecordStatus status) {
        User user = mock(User.class);
        when(user.getName()).thenReturn("근로자");

        Worker worker = mock(Worker.class);
        when(worker.getUser()).thenReturn(user);
        lenient().when(worker.getWorkerCode()).thenReturn("WORKER001");

        Workplace workplace = mock(Workplace.class);
        when(workplace.getName()).thenReturn("테스트 사업장");

        WorkerContract contract = mock(WorkerContract.class);
        when(contract.getId()).thenReturn(100L);
        when(contract.getWorker()).thenReturn(worker);
        when(contract.getWorkplace()).thenReturn(workplace);

        return WorkRecord.builder()
                .id(id)
                .contract(contract)
                .workDate(workDate)
                .startTime(startTime)
                .endTime(endTime)
                .breakMinutes(60)
                .status(status)
                .isModified(false)
                .build();
    }

}
