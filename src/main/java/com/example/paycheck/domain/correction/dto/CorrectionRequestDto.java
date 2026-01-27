package com.example.paycheck.domain.correction.dto;

import com.example.paycheck.domain.correction.entity.CorrectionRequest;
import com.example.paycheck.domain.correction.enums.CorrectionStatus;
import com.example.paycheck.domain.correction.enums.RequestType;
import com.example.paycheck.domain.user.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;
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
        @NotNull(message = "요청 타입은 필수입니다.")
        private RequestType type;

        // UPDATE/DELETE 타입에서 필요
        private Long workRecordId;

        // CREATE 타입에서 필요
        private Long contractId;

        @NotNull(message = "요청 근무 날짜는 필수입니다.")
        private LocalDate requestedWorkDate;

        @NotNull(message = "요청 시작 시간은 필수입니다.")
        @JsonFormat(pattern = "HH:mm")
        @Schema(type = "string", example = "09:00")
        private LocalTime requestedStartTime;

        @NotNull(message = "요청 종료 시간은 필수입니다.")
        @JsonFormat(pattern = "HH:mm")
        @Schema(type = "string", example = "18:00")
        private LocalTime requestedEndTime;

        // CREATE 타입에서 선택적
        private Integer requestedBreakMinutes;
        
        private String requestedMemo;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "CorrectionRequestResponse")
    public static class Response {
        private Long id;
        private RequestType type;
        private Long workRecordId;
        private Long contractId;
        private LocalDate originalWorkDate;
        @JsonFormat(pattern = "HH:mm")
        @Schema(type = "string", example = "09:00")
        private LocalTime originalStartTime;
        @JsonFormat(pattern = "HH:mm")
        @Schema(type = "string", example = "18:00")
        private LocalTime originalEndTime;
        private LocalDate requestedWorkDate;
        @JsonFormat(pattern = "HH:mm")
        @Schema(type = "string", example = "09:00")
        private LocalTime requestedStartTime;
        @JsonFormat(pattern = "HH:mm")
        @Schema(type = "string", example = "18:00")
        private LocalTime requestedEndTime;
        private Integer requestedBreakMinutes;
        private String requestedMemo;
        private CorrectionStatus status;
        private RequesterInfo requester;
        private LocalDateTime reviewedAt;
        private LocalDateTime createdAt;

        public static Response from(CorrectionRequest request) {
            return Response.builder()
                    .id(request.getId())
                    .type(request.getType())
                    .workRecordId(request.getWorkRecord() != null ? request.getWorkRecord().getId() : null)
                    .contractId(request.getContract() != null ? request.getContract().getId() : null)
                    // Entity에 저장된 원본 시간 사용 (approve 후에도 변경되지 않음)
                    .originalWorkDate(request.getOriginalWorkDate())
                    .originalStartTime(request.getOriginalStartTime())
                    .originalEndTime(request.getOriginalEndTime())
                    .requestedWorkDate(request.getRequestedWorkDate())
                    .requestedStartTime(request.getRequestedStartTime())
                    .requestedEndTime(request.getRequestedEndTime())
                    .requestedBreakMinutes(request.getRequestedBreakMinutes())
                    .requestedMemo(request.getRequestedMemo())
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
        private RequestType type;
        private Long workRecordId;
        private LocalDate workDate;
        @JsonFormat(pattern = "HH:mm")
        @Schema(type = "string", example = "09:00")
        private LocalTime originalStartTime;
        @JsonFormat(pattern = "HH:mm")
        @Schema(type = "string", example = "18:00")
        private LocalTime originalEndTime;
        @JsonFormat(pattern = "HH:mm")
        @Schema(type = "string", example = "09:00")
        private LocalTime requestedStartTime;
        @JsonFormat(pattern = "HH:mm")
        @Schema(type = "string", example = "18:00")
        private LocalTime requestedEndTime;
        private CorrectionStatus status;
        private RequesterInfo requester;
        private String workplaceName;
        private LocalDateTime createdAt;

        public static ListResponse from(CorrectionRequest request) {
            // workplace 이름 조회: CREATE 타입이면 contract에서, 그 외는 workRecord에서
            String workplaceName = null;
            if (request.getContract() != null) {
                workplaceName = request.getContract().getWorkplace().getName();
            } else if (request.getWorkRecord() != null) {
                workplaceName = request.getWorkRecord().getContract().getWorkplace().getName();
            }

            return ListResponse.builder()
                    .id(request.getId())
                    .type(request.getType())
                    .workRecordId(request.getWorkRecord() != null ? request.getWorkRecord().getId() : null)
                    // Entity에 저장된 원본 시간 사용 - CREATE 타입이면 requestedWorkDate 사용
                    .workDate(request.getOriginalWorkDate() != null ?
                             request.getOriginalWorkDate() :
                             request.getRequestedWorkDate())
                    .originalStartTime(request.getOriginalStartTime())
                    .originalEndTime(request.getOriginalEndTime())
                    .requestedStartTime(request.getRequestedStartTime())
                    .requestedEndTime(request.getRequestedEndTime())
                    .status(request.getStatus())
                    .requester(RequesterInfo.from(request.getRequester()))
                    .workplaceName(workplaceName)
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
