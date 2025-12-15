package com.example.wagemanager.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Spring MVC 설정
 * - Content Negotiation 전략을 JSON 우선으로 설정
 * - 컨트롤러는 JSON만 처리하도록 XML MessageConverter 제거
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

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // XML MessageConverter 제거 - 컨트롤러는 JSON만 처리
        // RestTemplate/WebClient에서는 별도로 XmlMapper를 설정하여 XML 파싱 가능
        // extendMessageConverters는 Spring Boot가 기본 컨버터를 모두 추가한 후에 호출됨
        converters.removeIf(converter -> converter instanceof MappingJackson2XmlHttpMessageConverter);
    }
}
