package com.example.paycheck.domain.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "WorkSchedule", description = "근무 스케줄 정보")
public class WorkScheduleDto {

    @NotNull(message = "요일은 필수입니다.")
    @Min(value = 1, message = "요일은 1(월요일)부터 7(일요일)까지입니다.")
    @Max(value = 7, message = "요일은 1(월요일)부터 7(일요일)까지입니다.")
    @Schema(description = "요일 (1: 월요일, 2: 화요일, ..., 7: 일요일)", example = "1")
    private Integer dayOfWeek;

    @NotNull(message = "시작 시간은 필수입니다.")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "시간 형식은 HH:mm 이어야 합니다.")
    @Schema(description = "근무 시작 시간 (HH:mm)", example = "09:00")
    private String startTime;

    @NotNull(message = "종료 시간은 필수입니다.")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "시간 형식은 HH:mm 이어야 합니다.")
    @Schema(description = "근무 종료 시간 (HH:mm)", example = "18:00")
    private String endTime;

    @Min(value = 0, message = "휴게시간은 0분 이상이어야 합니다.")
    @Schema(description = "휴게시간 (분)", example = "30")
    private Integer breakMinutes;
}
