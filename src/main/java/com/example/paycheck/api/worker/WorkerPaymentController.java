package com.example.paycheck.api.worker;

import com.example.paycheck.common.dto.ApiResponse;
import com.example.paycheck.common.exception.BadRequestException;
import com.example.paycheck.common.exception.ErrorCode;
import com.example.paycheck.domain.payment.dto.PaymentDto;
import com.example.paycheck.domain.payment.service.PaymentService;
import com.example.paycheck.domain.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "근로자 급여 송금", description = "근로자용 송금 내역 조회 API")
@RestController
@RequestMapping("/api/worker/payments")
@RequiredArgsConstructor
@PreAuthorize("@permissionEvaluator.isWorker()")
public class WorkerPaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "내 송금 내역 조회", description = "근로자 본인의 송금 내역을 조회합니다. year, month를 지정하면 해당 연월만 조회합니다.")
    @GetMapping
    public ApiResponse<List<PaymentDto.ListResponse>> getMyPayments(
            @AuthenticationPrincipal User user,
            @Parameter(description = "연도") @RequestParam(required = false) Integer year,
            @Parameter(description = "월") @RequestParam(required = false) Integer month) {
        if ((year == null) != (month == null)) {
            throw new BadRequestException(ErrorCode.INVALID_INPUT_VALUE, "year와 month는 함께 지정해야 합니다.");
        }
        if (year != null) {
            return ApiResponse.success(paymentService.getPaymentsByWorkerAndYearMonth(user.getId(), year, month));
        }
        return ApiResponse.success(paymentService.getPaymentsByWorker(user.getId()));
    }
}
