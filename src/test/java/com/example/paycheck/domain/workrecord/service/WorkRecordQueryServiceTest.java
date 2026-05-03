package com.example.paycheck.domain.workrecord.service;

import com.example.paycheck.common.exception.NotFoundException;
import com.example.paycheck.domain.correction.repository.CorrectionRequestRepository;
import com.example.paycheck.domain.contract.entity.WorkerContract;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.worker.entity.Worker;
import com.example.paycheck.domain.worker.repository.WorkerRepository;
import com.example.paycheck.domain.workrecord.dto.WorkRecordDto;
import com.example.paycheck.domain.workrecord.entity.WorkRecord;
import com.example.paycheck.domain.workrecord.enums.WorkRecordStatus;
import com.example.paycheck.domain.workrecord.repository.WorkRecordRepository;
import com.example.paycheck.domain.workplace.entity.Workplace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
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
        when(workRecordRepository.findByWorkplaceAndDateRange(workplaceId, startDate.minusDays(1), endDate, WorkRecordStatus.DELETED))
                .thenReturn(Arrays.asList());

        // when
        List<WorkRecordDto.CalendarResponse> result = workRecordQueryService
                .getWorkRecordsByWorkplaceAndDateRange(workplaceId, startDate, endDate);

        // then
        assertThat(result).isNotNull();
        verify(workRecordRepository).findByWorkplaceAndDateRange(workplaceId, startDate.minusDays(1), endDate, WorkRecordStatus.DELETED);
    }

    @Test
    @DisplayName("익일 근무는 종료일만 조회해도 타임라인에 표시된다")
    void getWorkRecordsByWorkplaceAndDateRange_IncludesOvernightRecordOnEndDate() {
        // given
        Long workplaceId = 1L;
        LocalDate startDate = LocalDate.of(2026, 3, 6);
        LocalDate endDate = LocalDate.of(2026, 3, 6);
        WorkRecord overnightRecord = createWorkRecord(
                1L,
                LocalDate.of(2026, 3, 5),
                LocalTime.of(23, 0),
                LocalTime.of(2, 0),
                WorkRecordStatus.SCHEDULED
        );

        when(workRecordRepository.findByWorkplaceAndDateRange(
                workplaceId, startDate.minusDays(1), endDate, WorkRecordStatus.DELETED))
                .thenReturn(List.of(overnightRecord));

        // when
        List<WorkRecordDto.CalendarResponse> result = workRecordQueryService
                .getWorkRecordsByWorkplaceAndDateRange(workplaceId, startDate, endDate);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getWorkDate()).isEqualTo(LocalDate.of(2026, 3, 6));
        verify(workRecordRepository).findByWorkplaceAndDateRange(
                workplaceId, startDate.minusDays(1), endDate, WorkRecordStatus.DELETED);
    }

    @Test
    @DisplayName("익일 근무는 시작일과 종료일 모두 타임라인에 매핑된다")
    void getWorkRecordsByWorkplaceAndDateRange_MapsOvernightRecordToBothDays() {
        // given
        Long workplaceId = 1L;
        LocalDate startDate = LocalDate.of(2026, 3, 5);
        LocalDate endDate = LocalDate.of(2026, 3, 6);
        WorkRecord overnightRecord = createWorkRecord(
                1L,
                LocalDate.of(2026, 3, 5),
                LocalTime.of(23, 0),
                LocalTime.of(2, 0),
                WorkRecordStatus.SCHEDULED
        );

        when(workRecordRepository.findByWorkplaceAndDateRange(
                workplaceId, startDate.minusDays(1), endDate, WorkRecordStatus.DELETED))
                .thenReturn(List.of(overnightRecord));

        // when
        List<WorkRecordDto.CalendarResponse> result = workRecordQueryService
                .getWorkRecordsByWorkplaceAndDateRange(workplaceId, startDate, endDate);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(WorkRecordDto.CalendarResponse::getWorkDate)
                .containsExactly(LocalDate.of(2026, 3, 5), LocalDate.of(2026, 3, 6));
        assertThat(result).extracting(WorkRecordDto.CalendarResponse::getId)
                .containsExactly(1L, 1L);
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
