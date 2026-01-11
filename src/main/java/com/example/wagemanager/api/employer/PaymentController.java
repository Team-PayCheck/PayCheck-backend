package com.example.wagemanager.api.employer;

import com.example.wagemanager.common.dto.ApiResponse;
import com.example.wagemanager.domain.payment.dto.PaymentDto;
import com.example.wagemanager.domain.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "고용주 급여 송금", description = "고용주용 급여 송금 관리 API")
@RestController
@RequestMapping("/api/employer/payments")
@RequiredArgsConstructor
@PreAuthorize("@userPermission.isEmployer()")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "급여 송금 처리", description = "근로자에게 토스 딥링크로 급여를 송금합니다.")
    @PreAuthorize("@salaryPermission.canAccess(#request.salaryId)")
    @PostMapping
    public ApiResponse<PaymentDto.Response> processPayment(
            @Valid @RequestBody PaymentDto.PaymentRequest request) {
        return ApiResponse.success(paymentService.processPayment(request));
    }

    @Operation(summary = "송금 상세 조회", description = "특정 송금 내역의 상세 정보를 조회합니다.")
    @PreAuthorize("@paymentPermission.canAccess(#id)")
    @GetMapping("/{id}")
    public ApiResponse<PaymentDto.Response> getPaymentById(
            @Parameter(description = "송금 ID", required = true) @PathVariable Long id) {
        return ApiResponse.success(paymentService.getPaymentById(id));
    }

    @Operation(summary = "사업장별 송금 목록 조회", description = "특정 사업장의 전체 송금 목록을 조회합니다.")
    @PreAuthorize("@paymentPermission.canAccessWorkplacePayments(#workplaceId)")
    @GetMapping
    public ApiResponse<List<PaymentDto.ListResponse>> getPaymentsByWorkplace(
            @Parameter(description = "사업장 ID", required = true) @RequestParam Long workplaceId) {
        return ApiResponse.success(paymentService.getPaymentsByWorkplace(workplaceId));
    }

    @Operation(summary = "사업장별 송금 목록 조회 (연월)", description = "특정 사업장의 특정 연월 송금 목록을 조회합니다.")
    @PreAuthorize("@paymentPermission.canAccessWorkplacePayments(#workplaceId)")
    @GetMapping("/year-month")
    public ApiResponse<List<PaymentDto.ListResponse>> getPaymentsByYearMonth(
            @Parameter(description = "사업장 ID", required = true) @RequestParam Long workplaceId,
            @Parameter(description = "연도", required = true) @RequestParam Integer year,
            @Parameter(description = "월", required = true) @RequestParam Integer month) {
        return ApiResponse.success(paymentService.getPaymentsByWorkplaceAndYearMonth(workplaceId, year, month));
    }

    @Operation(summary = "미송금자 목록 조회", description = "특정 연월의 미송금자 목록을 조회합니다. (PENDING + FAILED 상태)")
    @PreAuthorize("@paymentPermission.canAccessWorkplacePayments(#workplaceId)")
    @GetMapping("/unpaid")
    public ApiResponse<List<PaymentDto.ListResponse>> getUnpaidPayments(
            @Parameter(description = "사업장 ID", required = true) @RequestParam Long workplaceId,
            @Parameter(description = "연도", required = true) @RequestParam Integer year,
            @Parameter(description = "월", required = true) @RequestParam Integer month) {
        return ApiResponse.success(paymentService.getUnpaidPayments(workplaceId, year, month));
    }

    @Operation(summary = "송금 대기 목록 조회", description = "사업장의 모든 송금 대기 목록을 조회합니다. (PENDING 상태만)")
    @PreAuthorize("@paymentPermission.canAccessWorkplacePayments(#workplaceId)")
    @GetMapping("/pending")
    public ApiResponse<List<PaymentDto.ListResponse>> getPendingPaymentsByWorkplace(
            @Parameter(description = "사업장 ID", required = true) @RequestParam Long workplaceId) {
        return ApiResponse.success(paymentService.getPendingPaymentsByWorkplace(workplaceId));
    }

}
