package com.example.paycheck.domain.correction.service;

import com.example.paycheck.common.exception.BadRequestException;
import com.example.paycheck.common.exception.NotFoundException;
import com.example.paycheck.domain.allowance.entity.WeeklyAllowance;
import com.example.paycheck.domain.contract.entity.WorkerContract;
import com.example.paycheck.domain.contract.repository.WorkerContractRepository;
import com.example.paycheck.domain.correction.dto.CorrectionRequestDto;
import com.example.paycheck.domain.correction.entity.CorrectionRequest;
import com.example.paycheck.domain.correction.enums.CorrectionStatus;
import com.example.paycheck.domain.correction.repository.CorrectionRequestRepository;
import com.example.paycheck.domain.correction.service.CorrectionRequestService;
import com.example.paycheck.domain.notification.event.NotificationEvent;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.workrecord.entity.WorkRecord;
import com.example.paycheck.domain.workrecord.repository.WorkRecordRepository;
import com.example.paycheck.domain.correction.enums.RequestType;
import com.example.paycheck.domain.workrecord.enums.WorkRecordStatus;
import com.example.paycheck.domain.workrecord.service.WorkRecordCalculationService;
import com.example.paycheck.domain.workrecord.service.WorkRecordCommandService;
import com.example.paycheck.domain.workrecord.service.WorkRecordCoordinatorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CorrectionRequestService 테스트")
class CorrectionRequestServiceTest {

    @Mock
    private CorrectionRequestRepository correctionRequestRepository;

    @Mock
    private WorkRecordRepository workRecordRepository;

    @Mock
    private WorkerContractRepository workerContractRepository;

    @Mock
    private WorkRecordCommandService workRecordCommandService;

    @Mock
    private WorkRecordCalculationService calculationService;

    @Mock
    private WorkRecordCoordinatorService coordinatorService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CorrectionRequestService correctionRequestService;

    @Test
    @DisplayName("정정요청 생성 실패 - 근무 기록 없음 (UPDATE 타입)")
    void createCorrectionRequest_Fail_WorkRecordNotFound() {
        // given
        User requester = mock(User.class);
        CorrectionRequestDto.CreateRequest request = CorrectionRequestDto.CreateRequest.builder()
                .type(RequestType.UPDATE)
                .workRecordId(999L)
                .requestedWorkDate(LocalDate.now())
                .requestedStartTime(LocalTime.of(9, 0))
                .requestedEndTime(LocalTime.of(18, 0))
                .build();

        when(workRecordRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> correctionRequestService.createCorrectionRequest(requester, request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("정정요청 생성 실패 - 중복 요청 (UPDATE 타입)")
    void createCorrectionRequest_Fail_DuplicateRequest() {
        // given
        User requester = mock(User.class);
        WorkRecord workRecord = mock(WorkRecord.class);

        when(workRecord.getStatus()).thenReturn(WorkRecordStatus.COMPLETED);

        CorrectionRequestDto.CreateRequest request = CorrectionRequestDto.CreateRequest.builder()
                .type(RequestType.UPDATE)
                .workRecordId(1L)
                .requestedWorkDate(LocalDate.now())
                .requestedStartTime(LocalTime.of(9, 0))
                .requestedEndTime(LocalTime.of(18, 0))
                .build();

        when(workRecordRepository.findById(1L)).thenReturn(Optional.of(workRecord));
        when(correctionRequestRepository.existsByWorkRecordIdAndStatus(1L, CorrectionStatus.PENDING))
                .thenReturn(true);

        // when & then
        assertThatThrownBy(() -> correctionRequestService.createCorrectionRequest(requester, request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("내 정정요청 목록 조회 - 전체")
    void getMyCorrectionRequests_All() {
        // given
        User requester = mock(User.class);
        when(requester.getId()).thenReturn(1L);
        when(correctionRequestRepository.findByRequesterId(1L)).thenReturn(Arrays.asList());

        // when
        List<CorrectionRequestDto.ListResponse> result =
                correctionRequestService.getMyCorrectionRequests(requester, null);

        // then
        assertThat(result).isNotNull();
        verify(correctionRequestRepository).findByRequesterId(1L);
    }

    @Test
    @DisplayName("내 정정요청 목록 조회 - 상태별")
    void getMyCorrectionRequests_ByStatus() {
        // given
        User requester = mock(User.class);
        when(requester.getId()).thenReturn(1L);
        when(correctionRequestRepository.findByRequesterIdAndStatus(1L, CorrectionStatus.PENDING))
                .thenReturn(Arrays.asList());

        // when
        List<CorrectionRequestDto.ListResponse> result =
                correctionRequestService.getMyCorrectionRequests(requester, CorrectionStatus.PENDING);

        // then
        assertThat(result).isNotNull();
        verify(correctionRequestRepository).findByRequesterIdAndStatus(1L, CorrectionStatus.PENDING);
    }

    @Test
    @DisplayName("내 정정요청 상세 조회 실패 - 요청 없음")
    void getMyCorrectionRequest_Fail_NotFound() {
        // given
        User requester = mock(User.class);
        when(correctionRequestRepository.findByIdWithDetails(anyLong())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> correctionRequestService.getMyCorrectionRequest(requester, 1L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("정정요청 승인 시 근무일과 휴게시간이 수정되고 주차 재할당 및 재계산이 수행된다")
    void approveCorrectionRequest_UpdateWorkDateAndBreakMinutes() {
        // given
        User requester = mock(User.class);
        when(requester.getId()).thenReturn(10L);
        when(requester.getName()).thenReturn("근로자");

        WorkerContract contract = mock(WorkerContract.class);
        when(contract.getId()).thenReturn(100L);

        WeeklyAllowance oldWeeklyAllowance = WeeklyAllowance.builder()
                .contract(contract)
                .weekStartDate(LocalDate.of(2026, 2, 16))
                .weekEndDate(LocalDate.of(2026, 2, 22))
                .build();

        WeeklyAllowance newWeeklyAllowance = WeeklyAllowance.builder()
                .contract(contract)
                .weekStartDate(LocalDate.of(2026, 2, 23))
                .weekEndDate(LocalDate.of(2026, 3, 1))
                .build();

        WorkRecord workRecord = WorkRecord.builder()
                .id(1L)
                .contract(contract)
                .weeklyAllowance(oldWeeklyAllowance)
                .workDate(LocalDate.of(2026, 2, 21))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .breakMinutes(60)
                .totalWorkMinutes(480)
                .status(WorkRecordStatus.COMPLETED)
                .memo("기존 메모")
                .build();
        oldWeeklyAllowance.getWorkRecords().add(workRecord);

        CorrectionRequest correctionRequest = CorrectionRequest.builder()
                .id(1L)
                .type(RequestType.UPDATE)
                .workRecord(workRecord)
                .requester(requester)
                .originalWorkDate(workRecord.getWorkDate())
                .originalStartTime(workRecord.getStartTime())
                .originalEndTime(workRecord.getEndTime())
                .requestedWorkDate(LocalDate.of(2026, 2, 24))
                .requestedStartTime(LocalTime.of(10, 0))
                .requestedEndTime(LocalTime.of(19, 0))
                .requestedBreakMinutes(30)
                .requestedMemo("휴게시간 수정")
                .status(CorrectionStatus.PENDING)
                .build();

        when(correctionRequestRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(correctionRequest));
        when(coordinatorService.getOrCreateWeeklyAllowance(100L, LocalDate.of(2026, 2, 24)))
                .thenReturn(newWeeklyAllowance);

        // when
        CorrectionRequestDto.Response result = correctionRequestService.approveCorrectionRequest(1L);

        // then
        assertThat(result.getStatus()).isEqualTo(CorrectionStatus.APPROVED);
        assertThat(result.getRequestedWorkDate()).isEqualTo(LocalDate.of(2026, 2, 24));
        assertThat(result.getRequestedBreakMinutes()).isEqualTo(30);
        assertThat(workRecord.getWorkDate()).isEqualTo(LocalDate.of(2026, 2, 24));
        assertThat(workRecord.getBreakMinutes()).isEqualTo(30);
        assertThat(workRecord.getTotalWorkMinutes()).isEqualTo(510);
        assertThat(workRecord.getStartTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(workRecord.getEndTime()).isEqualTo(LocalTime.of(19, 0));
        assertThat(workRecord.getMemo()).isEqualTo("휴게시간 수정");
        assertThat(workRecord.getWeeklyAllowance()).isEqualTo(newWeeklyAllowance);
        assertThat(oldWeeklyAllowance.getWorkRecords()).doesNotContain(workRecord);
        assertThat(newWeeklyAllowance.getWorkRecords()).contains(workRecord);

        verify(workRecordRepository, times(2)).save(workRecord);
        verify(calculationService).calculateWorkRecordDetails(workRecord);
        verify(calculationService).validateWorkRecordConsistency(workRecord);
        verify(coordinatorService).getOrCreateWeeklyAllowance(100L, LocalDate.of(2026, 2, 24));
        verify(coordinatorService).handleWorkRecordUpdate(
                workRecord,
                oldWeeklyAllowance,
                newWeeklyAllowance,
                LocalDate.of(2026, 2, 21));
        verify(eventPublisher).publishEvent(any(NotificationEvent.class));
    }
}
