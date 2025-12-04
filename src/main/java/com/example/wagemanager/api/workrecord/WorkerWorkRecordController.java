package com.example.wagemanager.api.workrecord;

import com.example.wagemanager.common.dto.ApiResponse;
import com.example.wagemanager.domain.user.entity.User;
import com.example.wagemanager.domain.workrecord.dto.WorkRecordDto;
import com.example.wagemanager.domain.workrecord.service.WorkRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "근로자 근무 기록", description = "근로자용 근무 일정 및 기록 조회 API")
@RestController
@RequestMapping("/api/worker/work-records")
@RequiredArgsConstructor
public class WorkerWorkRecordController {

    private final WorkRecordService workRecordService;

    @Operation(summary = "내 근무 일정 조회", description = "로그인한 근로자의 기간별 근무 일정을 조회합니다.")
    @GetMapping
    public ApiResponse<List<WorkRecordDto.DetailedResponse>> getMyWorkRecords(
            @AuthenticationPrincipal User user,
            @Parameter(description = "조회 시작일 (yyyy-MM-dd)", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "조회 종료일 (yyyy-MM-dd)", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(
                workRecordService.getWorkRecordsByWorkerAndDateRange(user.getId(), startDate, endDate));
    }

    @Operation(summary = "근무 기록 상세 조회", description = "특정 근무 기록의 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ApiResponse<WorkRecordDto.DetailedResponse> getWorkRecord(
            @Parameter(description = "근무 기록 ID", required = true) @PathVariable Long id) {
        return ApiResponse.success(workRecordService.getWorkRecordById(id));
    }

    @Operation(summary = "근무 완료 처리", description = "근무를 완료 상태로 변경합니다.")
    @PutMapping("/{id}/complete")
    public ApiResponse<Void> completeWorkRecord(
            @Parameter(description = "근무 기록 ID", required = true) @PathVariable Long id) {
        workRecordService.completeWorkRecord(id);
        return ApiResponse.success(null);
    }
}
