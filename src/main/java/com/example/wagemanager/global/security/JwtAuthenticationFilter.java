package com.example.wagemanager.global.security;

import com.example.wagemanager.common.dto.ApiResponse;
import com.example.wagemanager.domain.user.entity.User;
import com.example.wagemanager.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 인증 필터
 * 모든 HTTP 요청을 인터셉트하여 JWT 토큰을 검증하고 Spring Security Context에 인증 정보를 설정
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 요청 헤더에서 JWT 토큰 추출
            String jwt = getJwtFromRequest(request);

            // 토큰이 있고 유효한 경우
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                // 토큰에서 userId 추출
                Long userId = tokenProvider.getUserIdFromToken(jwt);

                // DB에서 사용자 조회
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("User not found: " + userId));

                // UserDetails 생성
                CustomUserDetails userDetails = new CustomUserDetails(user);

                // Spring Security 인증 객체 생성
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user,  // Principal에 User 엔티티 저장 (@AuthenticationPrincipal로 접근 가능)
                                null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // SecurityContext에 인증 정보 설정
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);

            // 클라이언트에게 401 Unauthorized 응답 전달
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");

            ApiResponse<Void> errorResponse = ApiResponse.error(
                "UNAUTHORIZED",
                "인증에 실패했습니다. 유효한 토큰을 제공해주세요."
            );

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            return;
        }

        // 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }

    /**
     * HTTP 요청 헤더에서 JWT 토큰 추출
     * Authorization: Bearer {token} 형식
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
