package com.example.wagemanager.api.worker;

import com.example.wagemanager.common.dto.ApiResponse;
import com.example.wagemanager.domain.payment.dto.PaymentDto;
import com.example.wagemanager.domain.payment.service.PaymentService;
import com.example.wagemanager.domain.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
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
@PreAuthorize("@userPermission.isWorker()")
public class WorkerPaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "내 송금 내역 조회", description = "근로자 본인의 모든 송금 내역을 조회합니다.")
    @GetMapping
    public ApiResponse<List<PaymentDto.ListResponse>> getMyPayments(
            @AuthenticationPrincipal User user) {
        return ApiResponse.success(paymentService.getPaymentsByWorker(user.getId()));
    }
}
