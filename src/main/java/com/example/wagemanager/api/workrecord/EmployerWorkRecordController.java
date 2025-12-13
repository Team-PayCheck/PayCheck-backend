package com.example.wagemanager.api.workrecord;

import com.example.wagemanager.common.dto.ApiResponse;
import com.example.wagemanager.domain.workrecord.dto.WorkRecordDto;
import com.example.wagemanager.domain.workrecord.service.WorkRecordCommandService;
import com.example.wagemanager.domain.workrecord.service.WorkRecordQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "고용주 근무 기록", description = "고용주용 근무 일정 및 기록 관리 API")
@RestController
@RequestMapping("/api/employer/work-records")
@RequiredArgsConstructor
public class EmployerWorkRecordController {

    private final WorkRecordQueryService workRecordQueryService;
    private final WorkRecordCommandService workRecordCommandService;

    @Operation(summary = "근무 일정 등록", description = "고용주가 단일 근무 일정을 생성합니다. 생성 시 근로자에게 알람이 전송됩니다.")
    @PreAuthorize("@contractPermission.canAccessAsEmployer(#request.contractId)")
    @PostMapping
    public ApiResponse<WorkRecordDto.Response> createWorkRecord(
            @Valid @RequestBody WorkRecordDto.CreateRequest request) {
        return ApiResponse.success(workRecordCommandService.createWorkRecordByEmployer(request));
    }

    @Operation(summary = "근무 기록 조회 (캘린더)", description = "특정 사업장의 기간별 근무 기록을 캘린더 형식으로 조회합니다.")
    @PreAuthorize("@workRecordPermission.canAccessWorkplaceRecords(#workplaceId)")
    @GetMapping
    public ApiResponse<List<WorkRecordDto.CalendarResponse>> getWorkRecords(
            @Parameter(description = "사업장 ID", required = true) @RequestParam Long workplaceId,
            @Parameter(description = "조회 시작일 (yyyy-MM-dd)", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "조회 종료일 (yyyy-MM-dd)", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(
                workRecordQueryService.getWorkRecordsByWorkplaceAndDateRange(workplaceId, startDate, endDate));
    }

    @Operation(summary = "근무 기록 상세 조회", description = "특정 근무 기록의 상세 정보를 조회합니다.")
    @PreAuthorize("@workRecordPermission.canAccessAsEmployer(#id)")
    @GetMapping("/{id}")
    public ApiResponse<WorkRecordDto.DetailedResponse> getWorkRecord(
            @Parameter(description = "근무 기록 ID", required = true) @PathVariable Long id) {
        return ApiResponse.success(workRecordQueryService.getWorkRecordById(id));
    }

    @Operation(summary = "근무 일정 수정", description = "등록된 근무 일정 정보를 수정합니다. 수정 시 근로자에게 알람이 전송됩니다.")
    @PreAuthorize("@workRecordPermission.canAccessAsEmployer(#id)")
    @PutMapping("/{id}")
    public ApiResponse<WorkRecordDto.Response> updateWorkRecord(
            @Parameter(description = "근무 기록 ID", required = true) @PathVariable Long id,
            @RequestBody WorkRecordDto.UpdateRequest request) {
        return ApiResponse.success(workRecordCommandService.updateWorkRecord(id, request));
    }

    @Operation(summary = "근무 완료 처리", description = "근무 일정을 완료 상태로 변경합니다.")
    @PreAuthorize("@workRecordPermission.canAccessAsEmployer(#id)")
    @PutMapping("/{id}/complete")
    public ApiResponse<Void> completeWorkRecord(
            @Parameter(description = "근무 기록 ID", required = true) @PathVariable Long id) {
        workRecordCommandService.completeWorkRecord(id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "근무 일정 삭제", description = "등록된 근무 일정을 삭제합니다. 삭제 시 근로자에게 알람이 전송됩니다.")
    @PreAuthorize("@workRecordPermission.canAccessAsEmployer(#id)")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteWorkRecord(
            @Parameter(description = "근무 기록 ID", required = true) @PathVariable Long id) {
        workRecordCommandService.deleteWorkRecord(id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "근무 일정 승인", description = "근로자가 요청한 근무 일정을 승인합니다. 승인 시 근로자에게 알람이 전송됩니다.")
    @PreAuthorize("@workRecordPermission.canAccessAsEmployer(#id)")
    @PutMapping("/{id}/approve")
    public ApiResponse<WorkRecordDto.Response> approveWorkRecord(
            @Parameter(description = "근무 기록 ID", required = true) @PathVariable Long id) {
        return ApiResponse.success(workRecordCommandService.approveWorkRecord(id));
    }

    @Operation(summary = "근무 일정 거절", description = "근로자가 요청한 근무 일정을 거절합니다. 거절 시 근로자에게 알람이 전송됩니다.")
    @PreAuthorize("@workRecordPermission.canAccessAsEmployer(#id)")
    @PutMapping("/{id}/reject")
    public ApiResponse<Void> rejectWorkRecord(
            @Parameter(description = "근무 기록 ID", required = true) @PathVariable Long id) {
        workRecordCommandService.rejectWorkRecord(id);
        return ApiResponse.success(null);
    }
}
