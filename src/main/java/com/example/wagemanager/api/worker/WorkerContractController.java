package com.example.wagemanager.api.worker;

import com.example.wagemanager.common.dto.ApiResponse;
import com.example.wagemanager.domain.contract.dto.ContractDto;
import com.example.wagemanager.domain.contract.service.ContractService;
import com.example.wagemanager.domain.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "근로자 계약", description = "근로자용 근로 계약 조회 API")
@RestController
@RequestMapping("/api/worker/contracts")
@RequiredArgsConstructor
@PreAuthorize("@userPermission.isWorker()")
public class WorkerContractController {

    private final ContractService contractService;

    @Operation(summary = "내 계약 목록 조회", description = "로그인한 근로자의 계약 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<ContractDto.ListResponse>> getMyContracts(
            @AuthenticationPrincipal User user) {
        return ApiResponse.success(contractService.getContractsByUserId(user.getId()));
    }

    @Operation(summary = "계약 상세 조회", description = "특정 근로 계약의 상세 정보를 조회합니다.")
    @PreAuthorize("@contractPermission.canAccessAsWorker(#id)")
    @GetMapping("/{id}")
    public ApiResponse<ContractDto.Response> getContract(
            @Parameter(description = "계약 ID", required = true) @PathVariable Long id) {
        return ApiResponse.success(contractService.getContractById(id));
    }
}
