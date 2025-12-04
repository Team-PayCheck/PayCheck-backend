package com.example.wagemanager.api.employer;

import com.example.wagemanager.common.dto.ApiResponse;
import com.example.wagemanager.domain.contract.dto.ContractDto;
import com.example.wagemanager.domain.contract.service.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "고용주 계약", description = "고용주용 근로 계약 관리 API")
@RestController
@RequestMapping("/api/employer")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    @Operation(summary = "사업장에 근로자 추가", description = "사업장에 근로자를 추가하고 계약을 생성합니다.")
    @PostMapping("/workplaces/{workplaceId}/workers")
    public ApiResponse<ContractDto.Response> addWorkerToWorkplace(
            @Parameter(description = "사업장 ID", required = true) @PathVariable Long workplaceId,
            @Valid @RequestBody ContractDto.CreateRequest request) {
        return ApiResponse.success(contractService.addWorkerToWorkplace(workplaceId, request));
    }

    @Operation(summary = "사업장의 근로자 목록 조회", description = "특정 사업장에 소속된 근로자 목록을 조회합니다.")
    @GetMapping("/workplaces/{workplaceId}/workers")
    public ApiResponse<List<ContractDto.ListResponse>> getWorkplaceWorkers(
            @Parameter(description = "사업장 ID", required = true) @PathVariable Long workplaceId) {
        return ApiResponse.success(contractService.getContractsByWorkplaceId(workplaceId));
    }

    @Operation(summary = "계약 상세 조회", description = "특정 근로 계약의 상세 정보를 조회합니다.")
    @GetMapping("/contracts/{id}")
    public ApiResponse<ContractDto.Response> getContract(
            @Parameter(description = "계약 ID", required = true) @PathVariable Long id) {
        return ApiResponse.success(contractService.getContractById(id));
    }

    @Operation(summary = "계약 정보 수정", description = "근로 계약 정보를 수정합니다.")
    @PutMapping("/contracts/{id}")
    public ApiResponse<ContractDto.Response> updateContract(
            @Parameter(description = "계약 ID", required = true) @PathVariable Long id,
            @Valid @RequestBody ContractDto.UpdateRequest request) {
        return ApiResponse.success(contractService.updateContract(id, request));
    }

    @Operation(summary = "계약 종료", description = "근로 계약을 종료 처리합니다.")
    @DeleteMapping("/contracts/{id}")
    public ApiResponse<Void> terminateContract(
            @Parameter(description = "계약 ID", required = true) @PathVariable Long id) {
        contractService.terminateContract(id);
        return ApiResponse.success(null);
    }
}
