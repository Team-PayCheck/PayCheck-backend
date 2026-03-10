package com.example.paycheck.api.employer;

import com.example.paycheck.domain.salary.dto.SalaryDto;
import com.example.paycheck.domain.salary.service.SalaryService;
import com.example.paycheck.global.security.JwtAuthenticationFilter;
import com.example.paycheck.global.security.JwtTokenProvider;
import com.example.paycheck.global.security.permission.CustomPermissionEvaluator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployerSalaryController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("EmployerSalaryController 테스트")
class EmployerSalaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SalaryService salaryService;

    @MockitoBean
    private CustomPermissionEvaluator permissionEvaluator;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private SalaryDto.ListResponse createListResponse(Long id, String workerName, int year, int month) {
        return SalaryDto.ListResponse.builder()
                .id(id)
                .contractId(1L)
                .workerName(workerName)
                .year(year)
                .month(month)
                .totalGrossPay(BigDecimal.valueOf(2000000))
                .netPay(BigDecimal.valueOf(1800000))
                .paymentDueDate("2026-04-10")
                .build();
    }

    private SalaryDto.Response createDetailResponse(Long id) {
        return SalaryDto.Response.builder()
                .id(id)
                .contractId(1L)
                .workerId(10L)
                .workerName("홍길동")
                .workplaceId(5L)
                .workplaceName("테스트 사업장")
                .year(2026)
                .month(3)
                .totalWorkHours(BigDecimal.valueOf(160))
                .basePay(BigDecimal.valueOf(1600000))
                .overtimePay(BigDecimal.valueOf(100000))
                .nightPay(BigDecimal.ZERO)
                .holidayPay(BigDecimal.ZERO)
                .weeklyPaidLeaveAmount(BigDecimal.valueOf(300000))
                .totalGrossPay(BigDecimal.valueOf(2000000))
                .fourMajorInsurance(BigDecimal.valueOf(180000))
                .nationalPension(BigDecimal.valueOf(90000))
                .healthInsurance(BigDecimal.valueOf(70000))
                .longTermCare(BigDecimal.valueOf(8000))
                .employmentInsurance(BigDecimal.valueOf(12000))
                .incomeTax(BigDecimal.valueOf(15000))
                .localIncomeTax(BigDecimal.valueOf(1500))
                .totalDeduction(BigDecimal.valueOf(196500))
                .netPay(BigDecimal.valueOf(1803500))
                .paymentDueDate("2026-04-10")
                .build();
    }

    @Test
    @DisplayName("급여 목록 조회 - 성공")
    void getSalariesByWorkplace_success() throws Exception {
        // given
        List<SalaryDto.ListResponse> responses = List.of(
                createListResponse(1L, "홍길동", 2026, 3),
                createListResponse(2L, "김영희", 2026, 3)
        );

        given(salaryService.getSalariesByWorkplace(eq(1L))).willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/employer/salaries")
                        .param("workplaceId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(1L))
                .andExpect(jsonPath("$.data[0].workerName").value("홍길동"))
                .andExpect(jsonPath("$.data[0].totalGrossPay").value(2000000))
                .andExpect(jsonPath("$.data[1].id").value(2L))
                .andExpect(jsonPath("$.data[1].workerName").value("김영희"));
    }

    @Test
    @DisplayName("급여 목록 조회 (연월) - 성공")
    void getSalariesByYearMonth_success() throws Exception {
        // given
        List<SalaryDto.ListResponse> responses = List.of(
                createListResponse(1L, "홍길동", 2026, 3)
        );

        given(salaryService.getSalariesByWorkplaceAndYearMonth(eq(1L), eq(2026), eq(3)))
                .willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/employer/salaries/year-month")
                        .param("workplaceId", "1")
                        .param("year", "2026")
                        .param("month", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(1L))
                .andExpect(jsonPath("$.data[0].workerName").value("홍길동"))
                .andExpect(jsonPath("$.data[0].year").value(2026))
                .andExpect(jsonPath("$.data[0].month").value(3));
    }

    @Test
    @DisplayName("급여 상세 조회 - 성공")
    void getSalaryById_success() throws Exception {
        // given
        SalaryDto.Response response = createDetailResponse(1L);

        given(salaryService.getSalaryById(eq(1L))).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/employer/salaries/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.contractId").value(1L))
                .andExpect(jsonPath("$.data.workerName").value("홍길동"))
                .andExpect(jsonPath("$.data.workplaceName").value("테스트 사업장"))
                .andExpect(jsonPath("$.data.year").value(2026))
                .andExpect(jsonPath("$.data.month").value(3))
                .andExpect(jsonPath("$.data.totalWorkHours").value(160))
                .andExpect(jsonPath("$.data.basePay").value(1600000))
                .andExpect(jsonPath("$.data.overtimePay").value(100000))
                .andExpect(jsonPath("$.data.totalGrossPay").value(2000000))
                .andExpect(jsonPath("$.data.totalDeduction").value(196500))
                .andExpect(jsonPath("$.data.netPay").value(1803500))
                .andExpect(jsonPath("$.data.paymentDueDate").value("2026-04-10"));
    }

    @Test
    @DisplayName("급여 자동 계산 - 성공")
    void calculateSalaryByWorkRecords_success() throws Exception {
        // given
        SalaryDto.Response response = createDetailResponse(1L);

        given(salaryService.calculateSalaryByWorkRecords(eq(1L), eq(2026), eq(3)))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/employer/salaries/contracts/{contractId}/calculate", 1L)
                        .param("year", "2026")
                        .param("month", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.contractId").value(1L))
                .andExpect(jsonPath("$.data.workerName").value("홍길동"))
                .andExpect(jsonPath("$.data.totalGrossPay").value(2000000))
                .andExpect(jsonPath("$.data.netPay").value(1803500));
    }
}
