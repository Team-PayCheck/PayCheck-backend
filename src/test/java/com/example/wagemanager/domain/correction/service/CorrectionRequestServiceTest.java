package com.example.wagemanager.domain.correction.service;

import com.example.wagemanager.common.exception.BadRequestException;
import com.example.wagemanager.common.exception.NotFoundException;
import com.example.wagemanager.domain.correction.dto.CorrectionRequestDto;
import com.example.wagemanager.domain.correction.enums.CorrectionStatus;
import com.example.wagemanager.domain.correction.repository.CorrectionRequestRepository;
import com.example.wagemanager.domain.user.entity.User;
import com.example.wagemanager.domain.workrecord.entity.WorkRecord;
import com.example.wagemanager.domain.workrecord.repository.WorkRecordRepository;
import com.example.wagemanager.domain.correction.enums.RequestType;
import com.example.wagemanager.domain.workrecord.enums.WorkRecordStatus;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CorrectionRequestService 테스트")
class CorrectionRequestServiceTest {

    @Mock
    private CorrectionRequestRepository correctionRequestRepository;

    @Mock
    private WorkRecordRepository workRecordRepository;

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
}
