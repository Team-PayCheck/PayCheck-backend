package com.example.paycheck.api.employer;

import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.workplace.dto.WorkplaceDto;
import com.example.paycheck.domain.workplace.service.WorkplaceService;
import com.example.paycheck.global.security.JwtAuthenticationFilter;
import com.example.paycheck.global.security.JwtTokenProvider;
import com.example.paycheck.global.security.permission.CustomPermissionEvaluator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkplaceController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("WorkplaceController 테스트")
class WorkplaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkplaceService workplaceService;

    @MockitoBean
    private CustomPermissionEvaluator permissionEvaluator;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private UsernamePasswordAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("테스트 고용주")
                .build();
        authentication = new UsernamePasswordAuthenticationToken(testUser, null, List.of());

        given(permissionEvaluator.isEmployer()).willReturn(true);
    }

    @Nested
    @DisplayName("GET /api/employer/workplaces")
    class GetWorkplaces {

        @Test
        @DisplayName("성공 - isActive 파라미터 없이 모든 사업장을 조회한다")
        void getWorkplaces_NoParam_Success() throws Exception {
            // given
            List<WorkplaceDto.ListResponse> response = List.of(
                    WorkplaceDto.ListResponse.builder().id(1L).name("사업장1").isActive(true).build(),
                    WorkplaceDto.ListResponse.builder().id(2L).name("사업장2").isActive(false).build()
            );
            given(workplaceService.getWorkplacesByUserId(eq(1L), eq(null)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get("/api/employer/workplaces")
                            .principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(2));
        }

        @Test
        @DisplayName("성공 - isActive=true 파라미터로 활성 사업장만 조회한다")
        void getWorkplaces_ActiveTrue_Success() throws Exception {
            // given
            List<WorkplaceDto.ListResponse> response = List.of(
                    WorkplaceDto.ListResponse.builder().id(1L).name("사업장1").isActive(true).build()
            );
            given(workplaceService.getWorkplacesByUserId(eq(1L), eq(true)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get("/api/employer/workplaces")
                            .param("isActive", "true")
                            .principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].isActive").value(true));
        }

        @Test
        @DisplayName("성공 - isActive=false 파라미터로 비활성 사업장만 조회한다")
        void getWorkplaces_ActiveFalse_Success() throws Exception {
            // given
            List<WorkplaceDto.ListResponse> response = List.of(
                    WorkplaceDto.ListResponse.builder().id(2L).name("사업장2").isActive(false).build()
            );
            given(workplaceService.getWorkplacesByUserId(eq(1L), eq(false)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get("/api/employer/workplaces")
                            .param("isActive", "false")
                            .principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].isActive").value(false));
        }
    }
}
