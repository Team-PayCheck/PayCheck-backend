package com.example.paycheck.api.correctionrequest;

import com.example.paycheck.domain.correction.dto.CorrectionRequestDto;
import com.example.paycheck.domain.correction.enums.CorrectionStatus;
import com.example.paycheck.domain.correction.enums.RequestType;
import com.example.paycheck.domain.correction.service.CorrectionRequestService;
import com.example.paycheck.domain.workrecord.service.WorkRecordQueryService;
import com.example.paycheck.global.security.JwtAuthenticationFilter;
import com.example.paycheck.global.security.permission.CustomPermissionEvaluator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployerCorrectionRequestController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("EmployerCorrectionRequestController 테스트")
class EmployerCorrectionRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CorrectionRequestService correctionRequestService;

    @MockitoBean
    private WorkRecordQueryService workRecordQueryService;

    @MockitoBean
    private CustomPermissionEvaluator permissionEvaluator;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private CorrectionRequestDto.ListResponse createSampleListResponse(Long id, RequestType type, CorrectionStatus status) {
        return CorrectionRequestDto.ListResponse.builder()
                .id(id)
                .type(type)
                .workRecordId(100L)
                .workDate(LocalDate.of(2026, 3, 5))
                .originalStartTime(LocalTime.of(9, 0))
                .originalEndTime(LocalTime.of(18, 0))
                .requestedStartTime(LocalTime.of(10, 0))
                .requestedEndTime(LocalTime.of(19, 0))
                .status(status)
                .requester(CorrectionRequestDto.RequesterInfo.builder()
                        .id(200L)
                        .name("홍길동")
                        .build())
                .workplaceName("테스트 사업장")
                .createdAt(LocalDateTime.of(2026, 3, 8, 14, 30))
                .build();
    }

    private CorrectionRequestDto.Response createSampleResponse(Long id, CorrectionStatus status) {
        return CorrectionRequestDto.Response.builder()
                .id(id)
                .type(RequestType.UPDATE)
                .workRecordId(100L)
                .contractId(50L)
                .originalWorkDate(LocalDate.of(2026, 3, 5))
                .originalStartTime(LocalTime.of(9, 0))
                .originalEndTime(LocalTime.of(18, 0))
                .requestedWorkDate(LocalDate.of(2026, 3, 5))
                .requestedStartTime(LocalTime.of(10, 0))
                .requestedEndTime(LocalTime.of(19, 0))
                .requestedBreakMinutes(60)
                .requestedMemo("출근 시간 변경 요청")
                .status(status)
                .requester(CorrectionRequestDto.RequesterInfo.builder()
                        .id(200L)
                        .name("홍길동")
                        .build())
                .reviewedAt(status != CorrectionStatus.PENDING ? LocalDateTime.of(2026, 3, 9, 10, 0) : null)
                .createdAt(LocalDateTime.of(2026, 3, 8, 14, 30))
                .build();
    }

    @Nested
    @DisplayName("GET /api/employer/workplaces/{workplaceId}/pending-approvals")
    class GetPendingApprovals {

        @Test
        @DisplayName("성공 - 승인 대기중인 요청 목록을 조회한다")
        void getPendingApprovals_success() throws Exception {
            List<CorrectionRequestDto.ListResponse> pendingList = List.of(
                    createSampleListResponse(1L, RequestType.UPDATE, CorrectionStatus.PENDING),
                    createSampleListResponse(2L, RequestType.CREATE, CorrectionStatus.PENDING)
            );

            given(workRecordQueryService.getAllPendingApprovalsByWorkplace(eq(10L), isNull()))
                    .willReturn(pendingList);

            mockMvc.perform(get("/api/employer/workplaces/10/pending-approvals"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].id").value(1))
                    .andExpect(jsonPath("$.data[0].status").value("PENDING"))
                    .andExpect(jsonPath("$.data[1].type").value("CREATE"));
        }
    }

    @Nested
    @DisplayName("GET /api/employer/workplaces/{workplaceId}/correction-requests")
    class GetCorrectionRequests {

        @Test
        @DisplayName("성공 - 정정요청 목록을 페이징하여 조회한다")
        void getCorrectionRequests_success() throws Exception {
            List<CorrectionRequestDto.ListResponse> content = List.of(
                    createSampleListResponse(1L, RequestType.UPDATE, CorrectionStatus.APPROVED),
                    createSampleListResponse(2L, RequestType.DELETE, CorrectionStatus.PENDING)
            );
            PageRequest pageRequest = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<CorrectionRequestDto.ListResponse> page = new PageImpl<>(content, pageRequest, 2);

            given(correctionRequestService.getCorrectionRequestsByWorkplace(eq(10L), isNull(), any()))
                    .willReturn(page);

            mockMvc.perform(get("/api/employer/workplaces/10/correction-requests"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content.length()").value(2))
                    .andExpect(jsonPath("$.data.totalElements").value(2))
                    .andExpect(jsonPath("$.data.content[0].id").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/employer/correction-requests/{id}")
    class GetCorrectionRequestDetail {

        @Test
        @DisplayName("성공 - 정정요청 상세를 조회한다")
        void getCorrectionRequestDetail_success() throws Exception {
            given(correctionRequestService.getCorrectionRequestDetail(1L))
                    .willReturn(createSampleResponse(1L, CorrectionStatus.PENDING));

            mockMvc.perform(get("/api/employer/correction-requests/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.type").value("UPDATE"))
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andExpect(jsonPath("$.data.requestedMemo").value("출근 시간 변경 요청"))
                    .andExpect(jsonPath("$.data.requester.name").value("홍길동"));
        }
    }

    @Nested
    @DisplayName("PUT /api/employer/correction-requests/{id}/approve")
    class ApproveCorrectionRequest {

        @Test
        @DisplayName("성공 - 정정요청을 승인한다")
        void approveCorrectionRequest_success() throws Exception {
            given(correctionRequestService.approveCorrectionRequest(1L))
                    .willReturn(createSampleResponse(1L, CorrectionStatus.APPROVED));

            mockMvc.perform(put("/api/employer/correction-requests/1/approve"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.status").value("APPROVED"))
                    .andExpect(jsonPath("$.data.reviewedAt").isNotEmpty());
        }
    }

    @Nested
    @DisplayName("PUT /api/employer/correction-requests/{id}/reject")
    class RejectCorrectionRequest {

        @Test
        @DisplayName("성공 - 정정요청을 거절한다")
        void rejectCorrectionRequest_success() throws Exception {
            given(correctionRequestService.rejectCorrectionRequest(1L))
                    .willReturn(createSampleResponse(1L, CorrectionStatus.REJECTED));

            mockMvc.perform(put("/api/employer/correction-requests/1/reject"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.status").value("REJECTED"))
                    .andExpect(jsonPath("$.data.reviewedAt").isNotEmpty());
        }
    }
}
