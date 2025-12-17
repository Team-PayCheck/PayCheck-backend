package com.example.wagemanager.domain.workrecord.deletion.dto;

import com.example.wagemanager.domain.workrecord.deletion.entity.WorkRecordDeletionRequest;
import com.example.wagemanager.domain.workrecord.deletion.enums.WorkRecordDeletionRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class WorkRecordDeletionRequestDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "WorkRecordDeletionRequestCreateRequest")
    public static class CreateRequest {
        @Schema(description = "삭제 요청 사유", example = "일정이 잘못 등록됐습니다.", maxLength = 500)
        @Size(max = 500)
        private String reason;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "WorkRecordDeletionRequestResponse")
    public static class Response {
        private Long id;
        private Long workRecordId;
        private LocalDate workDate;
        private WorkRecordDeletionRequestStatus status;
        private String reason;
        private LocalDateTime reviewedAt;
        private LocalDateTime createdAt;

        public static Response from(WorkRecordDeletionRequest request) {
            return Response.builder()
                    .id(request.getId())
                    .workRecordId(request.getWorkRecord().getId())
                    .workDate(request.getWorkRecord().getWorkDate())
                    .status(request.getStatus())
                    .reason(request.getReason())
                    .reviewedAt(request.getReviewedAt())
                    .createdAt(request.getCreatedAt())
                    .build();
        }
    }
}
