package com.example.wagemanager.api.employer;

import com.example.wagemanager.common.dto.ApiResponse;
import com.example.wagemanager.domain.contract.dto.ContractDto;
import com.example.wagemanager.domain.contract.service.ContractService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employer")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    // 사업장에 근로자 추가 (계약 생성)
    @PostMapping("/workplaces/{workplaceId}/workers")
    public ApiResponse<ContractDto.Response> addWorkerToWorkplace(
            @PathVariable Long workplaceId,
            @Valid @RequestBody ContractDto.CreateRequest request) {
        return ApiResponse.success(contractService.addWorkerToWorkplace(workplaceId, request));
    }

    // 사업장의 근로자 목록 조회
    @GetMapping("/workplaces/{workplaceId}/workers")
    public ApiResponse<List<ContractDto.ListResponse>> getWorkplaceWorkers(
            @PathVariable Long workplaceId) {
        return ApiResponse.success(contractService.getContractsByWorkplaceId(workplaceId));
    }

    // 계약 상세 조회
    @GetMapping("/contracts/{id}")
    public ApiResponse<ContractDto.Response> getContract(@PathVariable Long id) {
        return ApiResponse.success(contractService.getContractById(id));
    }

    // 계약 수정
    @PutMapping("/contracts/{id}")
    public ApiResponse<ContractDto.Response> updateContract(
            @PathVariable Long id,
            @Valid @RequestBody ContractDto.UpdateRequest request) {
        return ApiResponse.success(contractService.updateContract(id, request));
    }

    // 계약 종료
    @DeleteMapping("/contracts/{id}")
    public ApiResponse<Void> terminateContract(@PathVariable Long id) {
        contractService.terminateContract(id);
        return ApiResponse.success(null);
    }
}
