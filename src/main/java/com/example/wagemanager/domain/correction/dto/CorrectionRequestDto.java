package com.example.wagemanager.domain.correction.dto;

import com.example.wagemanager.domain.correction.entity.CorrectionRequest;
import com.example.wagemanager.domain.correction.enums.CorrectionStatus;
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
        @NotNull
        private Long workRecordId;
        @NotNull
        private LocalDate requestedWorkDate;
        @NotNull
        private LocalTime requestedStartTime;
        @NotNull
        private LocalTime requestedEndTime;
        @NotNull
        private String reason;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "CorrectionRequestReviewRequest")
    public static class ReviewRequest {
        private String reviewComment;
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
        private String reason;
        private CorrectionStatus status;
        private RequesterInfo requester;
        private ReviewerInfo reviewer;
        private LocalDateTime reviewedAt;
        private String reviewComment;
        private LocalDateTime createdAt;

        public static Response from(CorrectionRequest request) {
            ResponseBuilder builder = Response.builder()
                    .id(request.getId())
                    .workRecordId(request.getWorkRecord().getId())
                    // Entity에 저장된 원본 시간 사용 (approve 후에도 변경되지 않음)
                    .originalWorkDate(request.getOriginalWorkDate())
                    .originalStartTime(request.getOriginalStartTime())
                    .originalEndTime(request.getOriginalEndTime())
                    .requestedWorkDate(request.getRequestedWorkDate())
                    .requestedStartTime(request.getRequestedStartTime())
                    .requestedEndTime(request.getRequestedEndTime())
                    .reason(request.getReason())
                    .status(request.getStatus())
                    .requester(RequesterInfo.builder()
                            .id(request.getRequester().getId())
                            .name(request.getRequester().getName())
                            .build())
                    .reviewedAt(request.getReviewedAt())
                    .reviewComment(request.getReviewComment())
                    .createdAt(request.getCreatedAt());

            if (request.getReviewer() != null) {
                builder.reviewer(ReviewerInfo.builder()
                        .id(request.getReviewer().getId())
                        .name(request.getReviewer().getName())
                        .build());
            }

            return builder.build();
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
        private String reason;
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
                    .reason(request.getReason())
                    .status(request.getStatus())
                    .requester(RequesterInfo.builder()
                            .id(request.getRequester().getId())
                            .name(request.getRequester().getName())
                            .build())
                    .workplaceName(request.getWorkRecord().getContract().getWorkplace().getName())
                    .createdAt(request.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "CorrectionRequestRequesterInfo")
    public static class RequesterInfo {
        private Long id;
        private String name;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "CorrectionRequestReviewerInfo")
    public static class ReviewerInfo {
        private Long id;
        private String name;
    }
}
