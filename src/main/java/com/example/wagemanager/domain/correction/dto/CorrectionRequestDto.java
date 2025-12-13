package com.example.wagemanager.domain.correction.dto;

import com.example.wagemanager.domain.correction.entity.CorrectionRequest;
import com.example.wagemanager.domain.correction.enums.CorrectionStatus;
import com.example.wagemanager.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class CorrectionRequestDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "CorrectionRequestCreateRequest")
    public static class CreateRequest {
        @NotNull(message = "근무 기록 ID는 필수입니다.")
        private Long workRecordId;

        @NotNull(message = "요청 근무 날짜는 필수입니다.")
        private LocalDate requestedWorkDate;

        @NotNull(message = "요청 시작 시간은 필수입니다.")
        private LocalTime requestedStartTime;

        @NotNull(message = "요청 종료 시간은 필수입니다.")
        private LocalTime requestedEndTime;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "CorrectionRequestResponse")
    public static class Response {
        private Long id;
        private Long workRecordId;
        private LocalDate originalWorkDate;
        private LocalTime originalStartTime;
        private LocalTime originalEndTime;
        private LocalDate requestedWorkDate;
        private LocalTime requestedStartTime;
        private LocalTime requestedEndTime;
        private CorrectionStatus status;
        private RequesterInfo requester;
        private LocalDateTime reviewedAt;
        private LocalDateTime createdAt;

        public static Response from(CorrectionRequest request) {
            return Response.builder()
                    .id(request.getId())
                    .workRecordId(request.getWorkRecord().getId())
                    // Entity에 저장된 원본 시간 사용 (approve 후에도 변경되지 않음)
                    .originalWorkDate(request.getOriginalWorkDate())
                    .originalStartTime(request.getOriginalStartTime())
                    .originalEndTime(request.getOriginalEndTime())
                    .requestedWorkDate(request.getRequestedWorkDate())
                    .requestedStartTime(request.getRequestedStartTime())
                    .requestedEndTime(request.getRequestedEndTime())
                    .status(request.getStatus())
                    .requester(RequesterInfo.from(request.getRequester()))
                    .reviewedAt(request.getReviewedAt())
                    .createdAt(request.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "CorrectionRequestListResponse")
    public static class ListResponse {
        private Long id;
        private Long workRecordId;
        private LocalDate workDate;
        private LocalTime originalStartTime;
        private LocalTime originalEndTime;
        private LocalTime requestedStartTime;
        private LocalTime requestedEndTime;
        private CorrectionStatus status;
        private RequesterInfo requester;
        private String workplaceName;
        private LocalDateTime createdAt;

        public static ListResponse from(CorrectionRequest request) {
            return ListResponse.builder()
                    .id(request.getId())
                    .workRecordId(request.getWorkRecord().getId())
                    // Entity에 저장된 원본 시간 사용
                    .workDate(request.getOriginalWorkDate())
                    .originalStartTime(request.getOriginalStartTime())
                    .originalEndTime(request.getOriginalEndTime())
                    .requestedStartTime(request.getRequestedStartTime())
                    .requestedEndTime(request.getRequestedEndTime())
                    .status(request.getStatus())
                    .requester(RequesterInfo.from(request.getRequester()))
                    .workplaceName(request.getWorkRecord().getContract().getWorkplace().getName())
                    .createdAt(request.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "RequesterInfo")
    public static class RequesterInfo {
        private Long id;
        private String name;

        public static RequesterInfo from(User user) {
            return RequesterInfo.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .build();
        }
    }
}
