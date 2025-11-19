package com.example.wagemanager.api.worker;

import com.example.wagemanager.common.dto.ApiResponse;
import com.example.wagemanager.domain.user.entity.User;
import com.example.wagemanager.domain.workrecord.dto.WorkRecordDto;
import com.example.wagemanager.domain.workrecord.service.WorkRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/worker/work-records")
@RequiredArgsConstructor
public class WorkerWorkRecordController {

    private final WorkRecordService workRecordService;

    // 내 근무 일정 조회
    @GetMapping
    public ApiResponse<List<WorkRecordDto.DetailedResponse>> getMyWorkRecords(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(
                workRecordService.getWorkRecordsByWorkerAndDateRange(user.getId(), startDate, endDate));
    }

    // 근무 기록 상세 조회
    @GetMapping("/{id}")
    public ApiResponse<WorkRecordDto.DetailedResponse> getWorkRecord(@PathVariable Long id) {
        return ApiResponse.success(workRecordService.getWorkRecordById(id));
    }

    // 근무 완료 처리
    @PutMapping("/{id}/complete")
    public ApiResponse<Void> completeWorkRecord(@PathVariable Long id) {
        workRecordService.completeWorkRecord(id);
        return ApiResponse.success(null);
    }
}
