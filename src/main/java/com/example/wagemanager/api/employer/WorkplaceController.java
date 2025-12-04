package com.example.wagemanager.api.employer;

import com.example.wagemanager.common.dto.ApiResponse;
import com.example.wagemanager.domain.user.entity.User;
import com.example.wagemanager.domain.workplace.dto.WorkplaceDto;
import com.example.wagemanager.domain.workplace.service.WorkplaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "고용주 사업장", description = "고용주용 사업장 관리 API")
@RestController
@RequestMapping("/api/employer/workplaces")
@RequiredArgsConstructor
public class WorkplaceController {

    private final WorkplaceService workplaceService;

    @Operation(summary = "사업장 등록", description = "새로운 사업장을 등록합니다.")
    @PostMapping
    public ApiResponse<WorkplaceDto.Response> createWorkplace(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody WorkplaceDto.CreateRequest request) {
        return ApiResponse.success(workplaceService.createWorkplace(user.getId(), request));
    }

    @Operation(summary = "내 사업장 목록 조회", description = "로그인한 고용주의 사업장 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<WorkplaceDto.ListResponse>> getWorkplaces(
            @AuthenticationPrincipal User user) {
        return ApiResponse.success(workplaceService.getWorkplacesByUserId(user.getId()));
    }

    @Operation(summary = "사업장 상세 조회", description = "특정 사업장의 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ApiResponse<WorkplaceDto.Response> getWorkplace(
            @Parameter(description = "사업장 ID", required = true) @PathVariable Long id) {
        return ApiResponse.success(workplaceService.getWorkplaceById(id));
    }

    @Operation(summary = "사업장 정보 수정", description = "사업장 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ApiResponse<WorkplaceDto.Response> updateWorkplace(
            @Parameter(description = "사업장 ID", required = true) @PathVariable Long id,
            @Valid @RequestBody WorkplaceDto.UpdateRequest request) {
        return ApiResponse.success(workplaceService.updateWorkplace(id, request));
    }

    @Operation(summary = "사업장 비활성화", description = "사업장을 비활성화(삭제) 처리합니다.")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deactivateWorkplace(
            @Parameter(description = "사업장 ID", required = true) @PathVariable Long id) {
        workplaceService.deactivateWorkplace(id);
        return ApiResponse.success(null);
    }
}
