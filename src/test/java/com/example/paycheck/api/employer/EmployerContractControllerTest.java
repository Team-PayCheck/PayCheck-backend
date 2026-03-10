package com.example.paycheck.api.employer;

import com.example.paycheck.domain.contract.dto.ContractDto;
import com.example.paycheck.domain.contract.service.ContractService;
import com.example.paycheck.domain.salary.util.DeductionCalculator;
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

import com.example.paycheck.domain.contract.dto.WorkScheduleDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployerContractController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("EmployerContractController 테스트")
class EmployerContractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContractService contractService;

    @MockitoBean
    private CustomPermissionEvaluator permissionEvaluator;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private ContractDto.Response createSampleResponse() {
        return ContractDto.Response.builder()
                .id(1L)
                .workplaceId(10L)
                .workplaceName("테스트 사업장")
                .workerId(100L)
                .workerName("홍길동")
                .workerCode("ABC123")
                .workerPhone("010-1234-5678")
                .hourlyWage(new BigDecimal("12000"))
                .workSchedules("[{\"day\":\"MONDAY\",\"startTime\":\"09:00\",\"endTime\":\"18:00\"}]")
                .contractStartDate(LocalDate.of(2026, 1, 1))
                .contractEndDate(LocalDate.of(2026, 12, 31))
                .paymentDay(15)
                .isActive(true)
                .payrollDeductionType(DeductionCalculator.PayrollDeductionType.PART_TIME_NONE)
                .build();
    }

    private ContractDto.ListResponse createSampleListResponse() {
        return ContractDto.ListResponse.builder()
                .id(1L)
                .workplaceId(10L)
                .workplaceName("테스트 사업장")
                .workerName("홍길동")
                .workerCode("ABC123")
                .workerPhone("010-1234-5678")
                .hourlyWage(new BigDecimal("12000"))
                .contractStartDate(LocalDate.of(2026, 1, 1))
                .contractEndDate(LocalDate.of(2026, 12, 31))
                .paymentDay(15)
                .payrollDeductionType(DeductionCalculator.PayrollDeductionType.PART_TIME_NONE)
                .isActive(true)
                .build();
    }

    @Nested
    @DisplayName("POST /api/employer/workplaces/{workplaceId}/workers")
    class AddWorkerToWorkplace {

        @Test
        @DisplayName("성공 - 사업장에 근로자를 추가한다")
        void addWorkerToWorkplace_success() throws Exception {
            WorkScheduleDto schedule = WorkScheduleDto.builder()
                    .dayOfWeek(1)
                    .startTime("09:00")
                    .endTime("18:00")
                    .breakMinutes(30)
                    .build();

            ContractDto.CreateRequest request = ContractDto.CreateRequest.builder()
                    .workerCode("ABC123")
                    .hourlyWage(new BigDecimal("12000"))
                    .workSchedules(List.of(schedule))
                    .contractStartDate(LocalDate.of(2026, 1, 1))
                    .paymentDay(15)
                    .payrollDeductionType(DeductionCalculator.PayrollDeductionType.PART_TIME_NONE)
                    .build();

            given(contractService.addWorkerToWorkplace(eq(10L), any(ContractDto.CreateRequest.class)))
                    .willReturn(createSampleResponse());

            mockMvc.perform(post("/api/employer/workplaces/10/workers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.workplaceId").value(10))
                    .andExpect(jsonPath("$.data.workerName").value("홍길동"))
                    .andExpect(jsonPath("$.data.hourlyWage").value(12000));
        }

        @Test
        @DisplayName("실패 - 필수 필드 누락 시 검증 오류 발생")
        void addWorkerToWorkplace_validationFailure() throws Exception {
            // workerCode, hourlyWage 등 필수 필드가 빠진 빈 요청
            String emptyRequest = "{}";

            mockMvc.perform(post("/api/employer/workplaces/10/workers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(emptyRequest))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/employer/contracts/{id}")
    class GetContractDetail {

        @Test
        @DisplayName("성공 - 계약 상세 정보를 조회한다")
        void getContractDetail_success() throws Exception {
            given(contractService.getContractById(1L))
                    .willReturn(createSampleResponse());

            mockMvc.perform(get("/api/employer/contracts/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.workplaceName").value("테스트 사업장"))
                    .andExpect(jsonPath("$.data.workerCode").value("ABC123"))
                    .andExpect(jsonPath("$.data.paymentDay").value(15))
                    .andExpect(jsonPath("$.data.isActive").value(true));
        }
    }

    @Nested
    @DisplayName("PUT /api/employer/contracts/{id}")
    class UpdateContract {

        @Test
        @DisplayName("성공 - 계약 정보를 수정한다")
        void updateContract_success() throws Exception {
            ContractDto.UpdateRequest request = ContractDto.UpdateRequest.builder()
                    .hourlyWage(new BigDecimal("15000"))
                    .paymentDay(20)
                    .build();

            ContractDto.Response updatedResponse = ContractDto.Response.builder()
                    .id(1L)
                    .workplaceId(10L)
                    .workplaceName("테스트 사업장")
                    .workerId(100L)
                    .workerName("홍길동")
                    .workerCode("ABC123")
                    .workerPhone("010-1234-5678")
                    .hourlyWage(new BigDecimal("15000"))
                    .workSchedules("[{\"day\":\"MONDAY\",\"startTime\":\"09:00\",\"endTime\":\"18:00\"}]")
                    .contractStartDate(LocalDate.of(2026, 1, 1))
                    .contractEndDate(LocalDate.of(2026, 12, 31))
                    .paymentDay(20)
                    .isActive(true)
                    .payrollDeductionType(DeductionCalculator.PayrollDeductionType.PART_TIME_NONE)
                    .build();

            given(contractService.updateContract(eq(1L), any(ContractDto.UpdateRequest.class)))
                    .willReturn(updatedResponse);

            mockMvc.perform(put("/api/employer/contracts/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.hourlyWage").value(15000))
                    .andExpect(jsonPath("$.data.paymentDay").value(20));
        }
    }

    @Nested
    @DisplayName("DELETE /api/employer/contracts/{id}")
    class TerminateContract {

        @Test
        @DisplayName("성공 - 계약을 종료한다")
        void terminateContract_success() throws Exception {
            willDoNothing().given(contractService).terminateContract(1L);

            mockMvc.perform(delete("/api/employer/contracts/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/employer/workplaces/{workplaceId}/workers")
    class GetWorkersByWorkplace {

        @Test
        @DisplayName("성공 - 사업장의 근로자 목록을 조회한다")
        void getWorkersByWorkplace_success() throws Exception {
            List<ContractDto.ListResponse> workers = List.of(
                    createSampleListResponse(),
                    ContractDto.ListResponse.builder()
                            .id(2L)
                            .workplaceId(10L)
                            .workplaceName("테스트 사업장")
                            .workerName("김철수")
                            .workerCode("DEF456")
                            .workerPhone("010-5678-1234")
                            .hourlyWage(new BigDecimal("11000"))
                            .contractStartDate(LocalDate.of(2026, 2, 1))
                            .paymentDay(25)
                            .payrollDeductionType(DeductionCalculator.PayrollDeductionType.FREELANCER)
                            .isActive(true)
                            .build()
            );

            given(contractService.getContractsByWorkplaceId(10L))
                    .willReturn(workers);

            mockMvc.perform(get("/api/employer/workplaces/10/workers"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].workerName").value("홍길동"))
                    .andExpect(jsonPath("$.data[1].workerName").value("김철수"));
        }
    }
}
