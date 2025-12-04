package com.example.wagemanager.domain.workrecord.dto;

import com.example.wagemanager.domain.workrecord.entity.WorkRecord;
import com.example.wagemanager.domain.workrecord.enums.WorkRecordStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
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
        @NotNull
        private Long contractId;
        @NotNull
        private LocalDate workDate;
        @NotNull
        private LocalTime startTime;
        @NotNull
        private LocalTime endTime;
        private Integer breakMinutes;
        private String memo;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "WorkRecordBatchCreateRequest")
    public static class BatchCreateRequest {
        @NotNull
        private Long contractId;
        @NotNull
        private List<LocalDate> workDates;
        @NotNull
        private LocalTime startTime;
        @NotNull
        private LocalTime endTime;
        private Integer breakMinutes;
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
        private Integer breakMinutes;
        private String memo;
    }
}
