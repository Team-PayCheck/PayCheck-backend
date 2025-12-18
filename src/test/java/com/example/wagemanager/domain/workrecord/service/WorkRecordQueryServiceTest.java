package com.example.wagemanager.domain.workrecord.service;

import com.example.wagemanager.common.exception.NotFoundException;
import com.example.wagemanager.domain.correction.repository.CorrectionRequestRepository;
import com.example.wagemanager.domain.worker.repository.WorkerRepository;
import com.example.wagemanager.domain.workrecord.dto.WorkRecordDto;
import com.example.wagemanager.domain.workrecord.repository.WorkRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
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
        when(workRecordRepository.findByContractId(contractId)).thenReturn(Arrays.asList());

        // when
        List<WorkRecordDto.Response> result = workRecordQueryService.getWorkRecordsByContract(contractId);

        // then
        assertThat(result).isNotNull();
        verify(workRecordRepository).findByContractId(contractId);
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
        when(workRecordRepository.findByWorkplaceAndDateRange(workplaceId, startDate, endDate))
                .thenReturn(Arrays.asList());

        // when
        List<WorkRecordDto.CalendarResponse> result = workRecordQueryService
                .getWorkRecordsByWorkplaceAndDateRange(workplaceId, startDate, endDate);

        // then
        assertThat(result).isNotNull();
        verify(workRecordRepository).findByWorkplaceAndDateRange(workplaceId, startDate, endDate);
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

}
