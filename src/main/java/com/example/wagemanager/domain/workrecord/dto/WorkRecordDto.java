package com.example.wagemanager.domain.workrecord.dto;

import com.example.wagemanager.domain.workrecord.entity.WorkRecord;
import com.example.wagemanager.domain.workrecord.enums.WorkRecordStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

public class WorkRecordDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
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
    public static class UpdateRequest {
        private LocalTime startTime;
        private LocalTime endTime;
        private Integer breakMinutes;
        private String memo;
    }
}
