package com.example.wagemanager.api.worker;

import com.example.wagemanager.common.dto.ApiResponse;
import com.example.wagemanager.domain.salary.dto.SalaryDto;
import com.example.wagemanager.domain.salary.service.SalaryService;
import com.example.wagemanager.domain.user.entity.User;
import com.example.wagemanager.domain.worker.entity.Worker;
import com.example.wagemanager.domain.worker.repository.WorkerRepository;
import com.example.wagemanager.common.exception.NotFoundException;
import com.example.wagemanager.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "근로자 급여", description = "근로자용 급여 조회 API")
@RestController
@RequestMapping("/api/worker/salaries")
@RequiredArgsConstructor
@PreAuthorize("@userPermission.isWorker()")
public class WorkerSalaryController {

    private final SalaryService salaryService;
    private final WorkerRepository workerRepository;

    @Operation(summary = "내 급여 목록 조회", description = "로그인한 근로자의 모든 급여 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<SalaryDto.ListResponse>> getMySalaries(
            @AuthenticationPrincipal User user) {
        Worker worker = workerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.WORKER_NOT_FOUND, "근로자 정보를 찾을 수 없습니다."));
        return ApiResponse.success(salaryService.getSalariesByWorker(worker.getId()));
    }

    @Operation(summary = "급여 상세 조회", description = "특정 급여의 상세 정보를 조회합니다.")
    @PreAuthorize("@salaryPermission.canAccessAsWorker(#id)")
    @GetMapping("/{id}")
    public ApiResponse<SalaryDto.Response> getSalaryById(
            @Parameter(description = "급여 ID", required = true) @PathVariable Long id) {
        return ApiResponse.success(salaryService.getSalaryById(id));
    }

    @Operation(summary = "급여 자동 계산", description = "근무 기록을 기반으로 급여를 자동 계산합니다. (세금/보험료 포함)")
    @PreAuthorize("@salaryPermission.canCalculateForContract(#contractId)")
    @PostMapping("/contracts/{contractId}/calculate")
    public ApiResponse<SalaryDto.Response> calculateSalaryByWorkRecords(
            @Parameter(description = "계약 ID", required = true) @PathVariable Long contractId,
            @Parameter(description = "연도", required = true) @RequestParam Integer year,
            @Parameter(description = "월", required = true) @RequestParam Integer month) {
        return ApiResponse.success(salaryService.calculateSalaryByWorkRecords(contractId, year, month));
    }
}
