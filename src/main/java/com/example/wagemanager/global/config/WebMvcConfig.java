package com.example.wagemanager.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 설정
 * Content Negotiation 전략을 JSON 우선으로 설정
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
                // 기본 Content Type을 JSON으로 설정
                .defaultContentType(MediaType.APPLICATION_JSON)
                // URL 파라미터 기반 Content Negotiation 비활성화
                .favorParameter(false)
                // Accept 헤더 무시 안 함 (클라이언트가 명시한 Accept 헤더 존중)
                .ignoreAcceptHeader(false)
                // Accept 헤더가 없을 때 사용할 기본 미디어 타입
                .defaultContentTypeStrategy(request -> {
                    return java.util.List.of(MediaType.APPLICATION_JSON);
                });
    }
}
