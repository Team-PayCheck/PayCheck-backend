package com.example.wagemanager.api.correctionrequest;

import com.example.wagemanager.common.dto.ApiResponse;
import com.example.wagemanager.domain.correction.dto.CorrectionRequestDto;
import com.example.wagemanager.domain.correction.enums.CorrectionStatus;
import com.example.wagemanager.domain.correction.service.CorrectionRequestService;
import com.example.wagemanager.domain.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "근로자 정정요청", description = "근로자용 근무 기록 정정요청 API")
@RestController
@RequestMapping("/api/worker/correction-requests")
@RequiredArgsConstructor
public class WorkerCorrectionRequestController {

    private final CorrectionRequestService correctionRequestService;

    @Operation(summary = "정정요청 생성", description = "근무 기록에 대한 정정요청을 생성합니다.")
    @PostMapping
    public ApiResponse<CorrectionRequestDto.Response> createCorrectionRequest(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CorrectionRequestDto.CreateRequest request) {
        return ApiResponse.success(
                correctionRequestService.createCorrectionRequest(user, request));
    }

    @Operation(summary = "내 정정요청 목록 조회", description = "로그인한 근로자의 정정요청 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<CorrectionRequestDto.ListResponse>> getMyCorrectionRequests(
            @AuthenticationPrincipal User user,
            @Parameter(description = "정정요청 상태 필터") @RequestParam(required = false) CorrectionStatus status) {
        return ApiResponse.success(
                correctionRequestService.getMyCorrectionRequests(user, status));
    }

    @Operation(summary = "내 정정요청 상세 조회", description = "특정 정정요청의 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ApiResponse<CorrectionRequestDto.Response> getMyCorrectionRequest(
            @AuthenticationPrincipal User user,
            @Parameter(description = "정정요청 ID", required = true) @PathVariable Long id) {
        return ApiResponse.success(
                correctionRequestService.getMyCorrectionRequest(user, id));
    }

    @Operation(summary = "정정요청 취소", description = "제출한 정정요청을 취소합니다.")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> cancelCorrectionRequest(
            @AuthenticationPrincipal User user,
            @Parameter(description = "정정요청 ID", required = true) @PathVariable Long id) {
        correctionRequestService.cancelCorrectionRequest(user, id);
        return ApiResponse.success(null);
    }
}
