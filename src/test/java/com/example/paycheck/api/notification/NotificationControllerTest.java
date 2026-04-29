package com.example.paycheck.api.notification;

import com.example.paycheck.domain.notification.service.NotificationService;
import com.example.paycheck.domain.notification.service.SseEmitterService;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.user.enums.UserType;
import com.example.paycheck.global.security.JwtAuthenticationFilter;
import com.example.paycheck.global.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("NotificationController 테스트")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private SseEmitterService sseEmitterService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("읽지 않은 알림 개수 조회 - 응답 포맷 검증")
    void getUnreadCount_ResponseFormat() throws Exception {
        // given
        given(notificationService.getUnreadCount(any(User.class))).willReturn(5L);

        User testUser = User.builder()
                .id(1L)
                .kakaoId("12345")
                .name("테스트")
                .userType(UserType.WORKER)
                .build();

        // when & then
        mockMvc.perform(get("/api/notifications/unread-count")
                        .requestAttr("user", testUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.count").value(5));
    }
}
