package com.example.paycheck.api.workrecord;

import com.example.paycheck.domain.workrecord.dto.WorkRecordDto;
import com.example.paycheck.domain.workrecord.enums.WorkRecordStatus;
import com.example.paycheck.domain.workrecord.service.WorkRecordCommandService;
import com.example.paycheck.domain.workrecord.service.WorkRecordQueryService;
import com.example.paycheck.global.security.JwtAuthenticationFilter;
import com.example.paycheck.global.security.JwtTokenProvider;
import com.example.paycheck.global.security.permission.CustomPermissionEvaluator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployerWorkRecordController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("EmployerWorkRecordController 테스트")
class EmployerWorkRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WorkRecordQueryService workRecordQueryService;

    @MockitoBean
    private WorkRecordCommandService workRecordCommandService;

    @MockitoBean
    private CustomPermissionEvaluator permissionEvaluator;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("근무 일정 등록 - 성공")
    void createWorkRecord_success() throws Exception {
        // given
        WorkRecordDto.CreateRequest request = WorkRecordDto.CreateRequest.builder()
                .contractId(1L)
                .workDate(LocalDate.of(2026, 3, 15))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .breakMinutes(60)
                .memo("테스트 메모")
                .build();

        WorkRecordDto.Response response = WorkRecordDto.Response.builder()
                .id(1L)
                .contractId(1L)
                .workDate(LocalDate.of(2026, 3, 15))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .breakMinutes(60)
                .totalWorkMinutes(480)
                .status(WorkRecordStatus.SCHEDULED)
                .memo("테스트 메모")
                .build();

        given(workRecordCommandService.createWorkRecordByEmployer(any(WorkRecordDto.CreateRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/employer/work-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.contractId").value(1L))
                .andExpect(jsonPath("$.data.workDate").value("2026-03-15"))
                .andExpect(jsonPath("$.data.breakMinutes").value(60))
                .andExpect(jsonPath("$.data.totalWorkMinutes").value(480))
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.memo").value("테스트 메모"));
    }

    @Test
    @DisplayName("근무 일정 등록 - 필수 필드 누락 시 400 에러")
    void createWorkRecord_validationFailure() throws Exception {
        // given - contractId, workDate, startTime, endTime 모두 누락
        String invalidRequest = "{}";

        // when & then
        mockMvc.perform(post("/api/employer/work-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("근무 기록 목록 조회 - 성공")
    void getWorkRecords_success() throws Exception {
        // given
        List<WorkRecordDto.CalendarResponse> responses = List.of(
                WorkRecordDto.CalendarResponse.builder()
                        .id(1L)
                        .contractId(1L)
                        .workerName("홍길동")
                        .workplaceName("테스트 사업장")
                        .workDate(LocalDate.of(2026, 3, 10))
                        .startTime(LocalTime.of(9, 0))
                        .endTime(LocalTime.of(18, 0))
                        .breakMinutes(60)
                        .hourlyWage(BigDecimal.valueOf(10000))
                        .status(WorkRecordStatus.COMPLETED)
                        .build(),
                WorkRecordDto.CalendarResponse.builder()
                        .id(2L)
                        .contractId(1L)
                        .workerName("홍길동")
                        .workplaceName("테스트 사업장")
                        .workDate(LocalDate.of(2026, 3, 11))
                        .startTime(LocalTime.of(9, 0))
                        .endTime(LocalTime.of(18, 0))
                        .breakMinutes(60)
                        .hourlyWage(BigDecimal.valueOf(10000))
                        .status(WorkRecordStatus.SCHEDULED)
                        .build()
        );

        given(workRecordQueryService.getWorkRecordsByWorkplaceAndDateRange(
                eq(1L),
                eq(LocalDate.of(2026, 3, 1)),
                eq(LocalDate.of(2026, 3, 31))))
                .willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/employer/work-records")
                        .param("workplaceId", "1")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(1L))
                .andExpect(jsonPath("$.data[0].workerName").value("홍길동"))
                .andExpect(jsonPath("$.data[0].workplaceName").value("테스트 사업장"))
                .andExpect(jsonPath("$.data[1].id").value(2L));
    }

    @Test
    @DisplayName("근무 기록 상세 조회 - 성공")
    void getWorkRecord_success() throws Exception {
        // given
        WorkRecordDto.DetailedResponse response = WorkRecordDto.DetailedResponse.builder()
                .id(1L)
                .contractId(1L)
                .workerName("홍길동")
                .workerCode("ABC123")
                .workplaceName("테스트 사업장")
                .workDate(LocalDate.of(2026, 3, 10))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .breakMinutes(60)
                .totalWorkMinutes(480)
                .status(WorkRecordStatus.COMPLETED)
                .isModified(false)
                .memo("메모")
                .hourlyWage(BigDecimal.valueOf(10000))
                .baseSalary(BigDecimal.valueOf(80000))
                .nightSalary(BigDecimal.ZERO)
                .holidaySalary(BigDecimal.ZERO)
                .totalSalary(BigDecimal.valueOf(80000))
                .build();

        given(workRecordQueryService.getWorkRecordById(1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/employer/work-records/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.workerName").value("홍길동"))
                .andExpect(jsonPath("$.data.workerCode").value("ABC123"))
                .andExpect(jsonPath("$.data.workplaceName").value("테스트 사업장"))
                .andExpect(jsonPath("$.data.totalWorkMinutes").value(480))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.baseSalary").value(80000))
                .andExpect(jsonPath("$.data.totalSalary").value(80000));
    }

    @Test
    @DisplayName("근무 일정 수정 - 성공")
    void updateWorkRecord_success() throws Exception {
        // given
        WorkRecordDto.UpdateRequest request = WorkRecordDto.UpdateRequest.builder()
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(19, 0))
                .breakMinutes(30)
                .memo("수정된 메모")
                .build();

        WorkRecordDto.Response response = WorkRecordDto.Response.builder()
                .id(1L)
                .contractId(1L)
                .workDate(LocalDate.of(2026, 3, 15))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(19, 0))
                .breakMinutes(30)
                .totalWorkMinutes(510)
                .status(WorkRecordStatus.SCHEDULED)
                .memo("수정된 메모")
                .build();

        given(workRecordCommandService.updateWorkRecord(eq(1L), any(WorkRecordDto.UpdateRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(put("/api/employer/work-records/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.startTime").value("10:00"))
                .andExpect(jsonPath("$.data.endTime").value("19:00"))
                .andExpect(jsonPath("$.data.breakMinutes").value(30))
                .andExpect(jsonPath("$.data.memo").value("수정된 메모"));
    }

    @Test
    @DisplayName("근무 일정 수정 - breakMinutes 음수 시 400 에러")
    void updateWorkRecord_validationFailure_negativeBreakMinutes() throws Exception {
        // given
        String invalidRequest = """
                {
                    "startTime": "10:00",
                    "endTime": "19:00",
                    "breakMinutes": -10,
                    "memo": "메모"
                }
                """;

        // when & then
        mockMvc.perform(put("/api/employer/work-records/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("근무 일정 삭제 - 성공")
    void deleteWorkRecord_success() throws Exception {
        // given
        willDoNothing().given(workRecordCommandService).deleteWorkRecord(1L);

        // when & then
        mockMvc.perform(delete("/api/employer/work-records/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("근무 완료 처리 - 성공")
    void completeWorkRecord_success() throws Exception {
        // given
        willDoNothing().given(workRecordCommandService).completeWorkRecord(1L);

        // when & then
        mockMvc.perform(put("/api/employer/work-records/{id}/complete", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
