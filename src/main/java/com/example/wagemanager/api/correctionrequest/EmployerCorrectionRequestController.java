package com.example.wagemanager.api.correctionrequest;

import com.example.wagemanager.common.dto.ApiResponse;
import com.example.wagemanager.domain.correction.dto.CorrectionRequestDto;
import com.example.wagemanager.domain.correction.enums.CorrectionStatus;
import com.example.wagemanager.domain.correction.service.CorrectionRequestService;
import com.example.wagemanager.domain.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "고용주 정정요청", description = "고용주용 근무 기록 정정요청 승인/거절 API")
@RestController
@RequestMapping("/api/employer")
@RequiredArgsConstructor
public class EmployerCorrectionRequestController {

    private final CorrectionRequestService correctionRequestService;

    @Operation(summary = "사업장별 정정요청 목록 조회", description = "특정 사업장의 정정요청 목록을 조회합니다.")
    @GetMapping("/workplaces/{workplaceId}/correction-requests")
    public ApiResponse<List<CorrectionRequestDto.ListResponse>> getCorrectionRequests(
            @Parameter(description = "사업장 ID", required = true) @PathVariable Long workplaceId,
            @Parameter(description = "정정요청 상태 필터") @RequestParam(required = false) CorrectionStatus status) {
        return ApiResponse.success(
                correctionRequestService.getCorrectionRequestsByWorkplace(workplaceId, status));
    }

    @Operation(summary = "정정요청 상세 조회", description = "특정 정정요청의 상세 정보를 조회합니다.")
    @GetMapping("/correction-requests/{id}")
    public ApiResponse<CorrectionRequestDto.Response> getCorrectionRequest(
            @Parameter(description = "정정요청 ID", required = true) @PathVariable Long id) {
        return ApiResponse.success(correctionRequestService.getCorrectionRequestDetail(id));
    }

    @Operation(summary = "정정요청 승인", description = "근로자의 정정요청을 승인하고 근무 기록을 수정합니다.")
    @PutMapping("/correction-requests/{id}/approve")
    public ApiResponse<CorrectionRequestDto.Response> approveCorrectionRequest(
            @AuthenticationPrincipal User user,
            @Parameter(description = "정정요청 ID", required = true) @PathVariable Long id,
            @RequestBody(required = false) CorrectionRequestDto.ReviewRequest request) {
        CorrectionRequestDto.ReviewRequest reviewRequest = request != null ? request : new CorrectionRequestDto.ReviewRequest();
        return ApiResponse.success(
                correctionRequestService.approveCorrectionRequest(user, id, reviewRequest));
    }

    @Operation(summary = "정정요청 거절", description = "근로자의 정정요청을 거절합니다.")
    @PutMapping("/correction-requests/{id}/reject")
    public ApiResponse<CorrectionRequestDto.Response> rejectCorrectionRequest(
            @AuthenticationPrincipal User user,
            @Parameter(description = "정정요청 ID", required = true) @PathVariable Long id,
            @RequestBody(required = false) CorrectionRequestDto.ReviewRequest request) {
        CorrectionRequestDto.ReviewRequest reviewRequest = request != null ? request : new CorrectionRequestDto.ReviewRequest();
        return ApiResponse.success(
                correctionRequestService.rejectCorrectionRequest(user, id, reviewRequest));
    }
}
