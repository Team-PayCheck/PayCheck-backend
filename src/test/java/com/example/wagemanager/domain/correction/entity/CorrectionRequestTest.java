package com.example.wagemanager.domain.correction.entity;

import com.example.wagemanager.domain.correction.enums.CorrectionStatus;
import com.example.wagemanager.domain.user.entity.User;
import com.example.wagemanager.domain.workrecord.entity.WorkRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("CorrectionRequest 엔티티 테스트")
class CorrectionRequestTest {

    private CorrectionRequest correctionRequest;
    private WorkRecord mockWorkRecord;
    private User mockRequester;

    @BeforeEach
    void setUp() {
        mockWorkRecord = mock(WorkRecord.class);
        mockRequester = mock(User.class);

        correctionRequest = CorrectionRequest.builder()
                .id(1L)
                .workRecord(mockWorkRecord)
                .requester(mockRequester)
                .originalWorkDate(LocalDate.of(2024, 1, 15))
                .originalStartTime(LocalTime.of(9, 0))
                .originalEndTime(LocalTime.of(18, 0))
                .requestedWorkDate(LocalDate.of(2024, 1, 15))
                .requestedStartTime(LocalTime.of(10, 0))
                .requestedEndTime(LocalTime.of(19, 0))
                .status(CorrectionStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("정정요청 승인 처리")
    void approve() {
        // when
        correctionRequest.approve();

        // then
        assertThat(correctionRequest.getStatus()).isEqualTo(CorrectionStatus.APPROVED);
        assertThat(correctionRequest.getReviewedAt()).isNotNull();
    }

    @Test
    @DisplayName("정정요청 거절 처리")
    void reject() {
        // when
        correctionRequest.reject();

        // then
        assertThat(correctionRequest.getStatus()).isEqualTo(CorrectionStatus.REJECTED);
        assertThat(correctionRequest.getReviewedAt()).isNotNull();
    }

    @Test
    @DisplayName("정정요청 승인 후 거절 불가 확인")
    void approve_Then_Reject() {
        // given
        correctionRequest.approve();
        var approvedTime = correctionRequest.getReviewedAt();

        // when
        correctionRequest.reject();

        // then
        assertThat(correctionRequest.getStatus()).isEqualTo(CorrectionStatus.REJECTED);
        assertThat(correctionRequest.getReviewedAt()).isAfterOrEqualTo(approvedTime);
    }
}
