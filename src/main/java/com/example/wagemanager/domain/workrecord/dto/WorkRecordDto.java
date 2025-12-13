package com.example.wagemanager.domain.workrecord.dto;

import com.example.wagemanager.domain.workrecord.entity.WorkRecord;
import com.example.wagemanager.domain.workrecord.enums.WorkRecordStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class WorkRecordDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "WorkRecordResponse")
    public static class Response {
        private Long id;
        private Long contractId;
        private LocalDate workDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private Integer breakMinutes;
        private Integer totalWorkMinutes;
        private WorkRecordStatus status;
        private String memo;

        public static Response from(WorkRecord workRecord) {
            return Response.builder()
                    .id(workRecord.getId())
                    .contractId(workRecord.getContract().getId())
                    .workDate(workRecord.getWorkDate())
                    .startTime(workRecord.getStartTime())
                    .endTime(workRecord.getEndTime())
                    .breakMinutes(workRecord.getBreakMinutes())
                    .totalWorkMinutes(workRecord.getTotalWorkMinutes())
                    .status(workRecord.getStatus())
                    .memo(workRecord.getMemo())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "WorkRecordDetailedResponse")
    public static class DetailedResponse {
        private Long id;
        private Long contractId;
        private String workerName;
        private String workerCode;
        private String workplaceName;
        private LocalDate workDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private Integer breakMinutes;
        private Integer totalWorkMinutes;
        private WorkRecordStatus status;
        private Boolean isModified;
        private String memo;

        public static DetailedResponse from(WorkRecord workRecord) {
            return DetailedResponse.builder()
                    .id(workRecord.getId())
                    .contractId(workRecord.getContract().getId())
                    .workerName(workRecord.getContract().getWorker().getUser().getName())
                    .workerCode(workRecord.getContract().getWorker().getWorkerCode())
                    .workplaceName(workRecord.getContract().getWorkplace().getName())
                    .workDate(workRecord.getWorkDate())
                    .startTime(workRecord.getStartTime())
                    .endTime(workRecord.getEndTime())
                    .breakMinutes(workRecord.getBreakMinutes())
                    .totalWorkMinutes(workRecord.getTotalWorkMinutes())
                    .status(workRecord.getStatus())
                    .isModified(workRecord.getIsModified())
                    .memo(workRecord.getMemo())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "WorkRecordCalendarResponse")
    public static class CalendarResponse {
        private Long id;
        private Long contractId;
        private String workerName;
        private String workplaceName;
        private LocalDate workDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private WorkRecordStatus status;

        public static CalendarResponse from(WorkRecord workRecord) {
            return CalendarResponse.builder()
                    .id(workRecord.getId())
                    .contractId(workRecord.getContract().getId())
                    .workerName(workRecord.getContract().getWorker().getUser().getName())
                    .workplaceName(workRecord.getContract().getWorkplace().getName())
                    .workDate(workRecord.getWorkDate())
                    .startTime(workRecord.getStartTime())
                    .endTime(workRecord.getEndTime())
                    .status(workRecord.getStatus())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "WorkRecordCreateRequest")
    public static class CreateRequest {
        @NotNull(message = "계약 ID는 필수입니다.")
        private Long contractId;

        @NotNull(message = "근무일은 필수입니다.")
        private LocalDate workDate;

        @NotNull(message = "시작 시간은 필수입니다.")
        private LocalTime startTime;

        @NotNull(message = "종료 시간은 필수입니다.")
        private LocalTime endTime;

        @Min(value = 0, message = "휴게 시간은 0분 이상이어야 합니다.")
        private Integer breakMinutes;

        @Size(max = 500, message = "메모는 500자 이하로 입력해주세요.")
        private String memo;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "WorkRecordBatchCreateRequest")
    public static class BatchCreateRequest {
        @NotNull(message = "계약 ID는 필수입니다.")
        private Long contractId;

        @NotEmpty(message = "근무일 목록은 비어있을 수 없습니다.")
        private List<LocalDate> workDates;

        @NotNull(message = "시작 시간은 필수입니다.")
        private LocalTime startTime;

        @NotNull(message = "종료 시간은 필수입니다.")
        private LocalTime endTime;

        @Min(value = 0, message = "휴게 시간은 0분 이상이어야 합니다.")
        private Integer breakMinutes;

        @Size(max = 500, message = "메모는 500자 이하로 입력해주세요.")
        private String memo;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "WorkRecordUpdateRequest")
    public static class UpdateRequest {
        private LocalTime startTime;

        private LocalTime endTime;

        @Min(value = 0, message = "휴게 시간은 0분 이상이어야 합니다.")
        private Integer breakMinutes;

        @Size(max = 500, message = "메모는 500자 이하로 입력해주세요.")
        private String memo;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "WorkRecordApprovalRequest", description = "근무 일정 승인/거절 요청")
    public static class ApprovalRequest {
        @NotNull(message = "승인 여부는 필수입니다.")
        @Schema(description = "승인 여부 (true: 승인, false: 거절)", example = "true")
        private Boolean approved;
    }
}
