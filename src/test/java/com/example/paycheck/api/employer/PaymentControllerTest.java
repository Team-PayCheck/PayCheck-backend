package com.example.paycheck.api.employer;

import com.example.paycheck.domain.payment.dto.PaymentDto;
import com.example.paycheck.domain.payment.enums.PaymentStatus;
import com.example.paycheck.domain.payment.service.PaymentService;
import com.example.paycheck.global.security.JwtAuthenticationFilter;
import com.example.paycheck.global.security.permission.CustomPermissionEvaluator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("PaymentController 테스트")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private CustomPermissionEvaluator permissionEvaluator;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private PaymentDto.Response createSampleResponse(Long id, PaymentStatus status) {
        return PaymentDto.Response.builder()
                .id(id)
                .salaryId(10L)
                .workerId(100L)
                .workerName("홍길동")
                .workplaceId(5L)
                .workplaceName("테스트 사업장")
                .year(2026)
                .month(2)
                .netPay(new BigDecimal("1800000"))
                .status(status)
                .paymentDate(status == PaymentStatus.COMPLETED ? "2026-03-15" : null)
                .transactionId(status == PaymentStatus.COMPLETED ? "TXN-001" : null)
                .failureReason(null)
                .isPaid(status == PaymentStatus.COMPLETED)
                .tossLink("supertoss://send?amount=1800000&bank=신한&accountNo=110123456789")
                .build();
    }

    private PaymentDto.ListResponse createSampleListResponse(Long id, PaymentStatus status) {
        return PaymentDto.ListResponse.builder()
                .id(id)
                .salaryId(10L)
                .workerName("홍길동")
                .workplaceId(5L)
                .workplaceName("테스트 사업장")
                .year(2026)
                .month(2)
                .netPay(new BigDecimal("1800000"))
                .status(status)
                .paymentDate(status == PaymentStatus.COMPLETED ? "2026-03-15" : null)
                .isPaid(status == PaymentStatus.COMPLETED)
                .build();
    }

    @Nested
    @DisplayName("POST /api/employer/payments")
    class CreatePayment {

        @Test
        @DisplayName("성공 - 급여 송금을 처리한다")
        void createPayment_success() throws Exception {
            PaymentDto.PaymentRequest request = PaymentDto.PaymentRequest.builder()
                    .salaryId(10L)
                    .build();

            given(paymentService.processPayment(any(PaymentDto.PaymentRequest.class)))
                    .willReturn(createSampleResponse(1L, PaymentStatus.PENDING));

            mockMvc.perform(post("/api/employer/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.salaryId").value(10))
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andExpect(jsonPath("$.data.tossLink").isNotEmpty())
                    .andExpect(jsonPath("$.data.isPaid").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/employer/payments/{id}")
    class GetPaymentDetail {

        @Test
        @DisplayName("성공 - 송금 상세 정보를 조회한다")
        void getPaymentDetail_success() throws Exception {
            given(paymentService.getPaymentById(1L))
                    .willReturn(createSampleResponse(1L, PaymentStatus.COMPLETED));

            mockMvc.perform(get("/api/employer/payments/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.workerName").value("홍길동"))
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.data.isPaid").value(true))
                    .andExpect(jsonPath("$.data.transactionId").value("TXN-001"));
        }
    }

    @Nested
    @DisplayName("GET /api/employer/payments")
    class GetPayments {

        @Test
        @DisplayName("성공 - 사업장별 송금 목록을 조회한다")
        void getPayments_success() throws Exception {
            List<PaymentDto.ListResponse> payments = List.of(
                    createSampleListResponse(1L, PaymentStatus.COMPLETED),
                    createSampleListResponse(2L, PaymentStatus.PENDING)
            );

            given(paymentService.getPaymentsByWorkplace(5L))
                    .willReturn(payments);

            mockMvc.perform(get("/api/employer/payments")
                            .param("workplaceId", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].status").value("COMPLETED"))
                    .andExpect(jsonPath("$.data[1].status").value("PENDING"));
        }
    }

    @Nested
    @DisplayName("PUT /api/employer/payments/{id}/complete")
    class CompletePayment {

        @Test
        @DisplayName("성공 - 송금 완료 처리한다")
        void completePayment_success() throws Exception {
            given(paymentService.completePayment(1L))
                    .willReturn(createSampleResponse(1L, PaymentStatus.COMPLETED));

            mockMvc.perform(put("/api/employer/payments/1/complete"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.data.isPaid").value(true))
                    .andExpect(jsonPath("$.data.paymentDate").isNotEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/employer/payments/unpaid")
    class GetUnpaidWorkers {

        @Test
        @DisplayName("성공 - 미송금자 목록을 조회한다")
        void getUnpaidWorkers_success() throws Exception {
            List<PaymentDto.ListResponse> unpaidPayments = List.of(
                    createSampleListResponse(1L, PaymentStatus.PENDING),
                    PaymentDto.ListResponse.builder()
                            .id(2L)
                            .salaryId(11L)
                            .workerName("김철수")
                            .workplaceId(5L)
                            .workplaceName("테스트 사업장")
                            .year(2026)
                            .month(2)
                            .netPay(new BigDecimal("950000"))
                            .status(PaymentStatus.FAILED)
                            .isPaid(false)
                            .build()
            );

            given(paymentService.getUnpaidPayments(5L, 2026, 2))
                    .willReturn(unpaidPayments);

            mockMvc.perform(get("/api/employer/payments/unpaid")
                            .param("workplaceId", "5")
                            .param("year", "2026")
                            .param("month", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].isPaid").value(false))
                    .andExpect(jsonPath("$.data[1].workerName").value("김철수"))
                    .andExpect(jsonPath("$.data[1].status").value("FAILED"));
        }
    }
}
