package com.example.wagemanager.api.employer;

import com.example.wagemanager.common.dto.ApiResponse;
import com.example.wagemanager.domain.salary.dto.SalaryDto;
import com.example.wagemanager.domain.salary.service.SalaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "고용주 급여", description = "고용주용 급여 계산 및 조회 API")
@RestController
@RequestMapping("/api/employer/salaries")
@RequiredArgsConstructor
public class SalaryController {

    private final SalaryService salaryService;

    @Operation(summary = "급여 목록 조회", description = "특정 사업장의 전체 급여 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<SalaryDto.ListResponse>> getSalariesByWorkplace(
            @Parameter(description = "사업장 ID", required = true) @RequestParam Long workplaceId) {
        return ApiResponse.success(salaryService.getSalariesByWorkplace(workplaceId));
    }

    @Operation(summary = "급여 목록 조회 (연월)", description = "특정 사업장의 특정 연월 급여 목록을 조회합니다.")
    @GetMapping("/year-month")
    public ApiResponse<List<SalaryDto.ListResponse>> getSalariesByYearMonth(
            @Parameter(description = "사업장 ID", required = true) @RequestParam Long workplaceId,
            @Parameter(description = "연도", required = true) @RequestParam Integer year,
            @Parameter(description = "월", required = true) @RequestParam Integer month) {
        return ApiResponse.success(salaryService.getSalariesByWorkplaceAndYearMonth(workplaceId, year, month));
    }

    @Operation(summary = "급여 상세 조회", description = "특정 급여의 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ApiResponse<SalaryDto.Response> getSalaryById(
            @Parameter(description = "급여 ID", required = true) @PathVariable Long id) {
        return ApiResponse.success(salaryService.getSalaryById(id));
    }

    @Operation(summary = "급여 자동 계산", description = "근무 기록을 기반으로 급여를 자동 계산합니다. (세금/보험료 포함)")
    @PostMapping("/contracts/{contractId}/calculate")
    public ApiResponse<SalaryDto.Response> calculateSalaryByWorkRecords(
            @Parameter(description = "계약 ID", required = true) @PathVariable Long contractId,
            @Parameter(description = "연도", required = true) @RequestParam Integer year,
            @Parameter(description = "월", required = true) @RequestParam Integer month) {
        return ApiResponse.success(salaryService.calculateSalaryByWorkRecords(contractId, year, month));
    }
}
