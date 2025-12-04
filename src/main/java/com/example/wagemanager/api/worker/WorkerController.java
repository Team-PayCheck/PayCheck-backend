package com.example.wagemanager.api.worker;

import com.example.wagemanager.common.dto.ApiResponse;
import com.example.wagemanager.domain.worker.dto.WorkerDto;
import com.example.wagemanager.domain.worker.service.WorkerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "근로자", description = "근로자 정보 조회 및 관리 API")
@RestController
@RequestMapping("/api/workers")
@RequiredArgsConstructor
public class WorkerController {

    private final WorkerService workerService;

    @Operation(summary = "근로자 조회 (ID)", description = "근로자 ID로 근로자 정보를 조회합니다.")
    @GetMapping("/{workerId}")
    public ApiResponse<WorkerDto.Response> getWorkerById(
            @Parameter(description = "근로자 ID", required = true) @PathVariable Long workerId) {
        return ApiResponse.success(workerService.getWorkerById(workerId));
    }

    @Operation(summary = "근로자 조회 (사용자 ID)", description = "사용자 ID로 근로자 정보를 조회합니다.")
    @GetMapping("/user/{userId}")
    public ApiResponse<WorkerDto.Response> getWorkerByUserId(
            @Parameter(description = "사용자 ID", required = true) @PathVariable Long userId) {
        return ApiResponse.success(workerService.getWorkerByUserId(userId));
    }

    @Operation(summary = "근로자 조회 (근로자 코드)", description = "근로자 코드로 근로자 정보를 조회합니다.")
    @GetMapping("/code/{workerCode}")
    public ApiResponse<WorkerDto.Response> getWorkerByCode(
            @Parameter(description = "근로자 코드", required = true) @PathVariable String workerCode) {
        return ApiResponse.success(workerService.getWorkerByWorkerCode(workerCode));
    }

    @Operation(summary = "근로자 정보 수정", description = "근로자 정보를 수정합니다.")
    @PutMapping("/{workerId}")
    public ApiResponse<WorkerDto.Response> updateWorker(
            @Parameter(description = "근로자 ID", required = true) @PathVariable Long workerId,
            @RequestBody WorkerDto.UpdateRequest request) {
        return ApiResponse.success(workerService.updateWorker(workerId, request));
    }
}
