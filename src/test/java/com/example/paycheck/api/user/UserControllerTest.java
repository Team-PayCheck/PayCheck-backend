package com.example.paycheck.api.user;

import com.example.paycheck.domain.user.dto.UserDto;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.user.enums.UserType;
import com.example.paycheck.domain.user.service.UserService;
import com.example.paycheck.domain.worker.service.WorkerService;
import com.example.paycheck.global.security.JwtAuthenticationFilter;
import com.example.paycheck.global.security.JwtTokenProvider;
import com.example.paycheck.global.security.permission.CustomPermissionEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserController 테스트")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private WorkerService workerService;

    @MockitoBean
    private CustomPermissionEvaluator permissionEvaluator;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .kakaoId("12345")
                .name("테스트")
                .userType(UserType.WORKER)
                .build();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(testUser, null, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        given(permissionEvaluator.canAccessUser(anyLong())).willReturn(true);
    }

    @Test
    @DisplayName("프로필 이미지 업로드 - 성공")
    void uploadMyProfileImage_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "profile.png",
                "image/png",
                "image-content".getBytes()
        );

        given(userService.uploadProfileImage(eq(1L), any()))
                .willReturn(UserDto.ProfileImageUploadResponse.from(
                        "https://test-bucket.s3.ap-northeast-2.amazonaws.com/profiles/1/profile.png"
                ));

        mockMvc.perform(multipart("/api/users/me/profile-image").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.profileImageUrl").value(
                        "https://test-bucket.s3.ap-northeast-2.amazonaws.com/profiles/1/profile.png"
                ));
    }
}
