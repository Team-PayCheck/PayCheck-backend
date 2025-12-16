package com.example.wagemanager.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Spring MVC 설정
 * - Content Negotiation 전략을 JSON 우선으로 설정
 * - 컨트롤러는 JSON만 처리하도록 XML MessageConverter 제거
 *
 * 문제: jackson-dataformat-xml 의존성이 있으면 Spring Boot가 자동으로
 * MappingJackson2XmlHttpMessageConverter를 등록하여 JSON 요청이 XML로 파싱됨
 *
 * 해결: @EnableWebMvc로 Spring Boot 자동 설정 비활성화 후
 * configureMessageConverters로 필요한 컨버터만 명시적으로 등록 (XML 제외)
 */
@Configuration
@EnableWebMvc
public class WebMvcConfig implements WebMvcConfigurer {

    private final ObjectMapper objectMapper;

    public WebMvcConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
                .defaultContentType(MediaType.APPLICATION_JSON)
                .favorParameter(false)
                .ignoreAcceptHeader(false);
    }

    /**
     * 메시지 컨버터를 명시적으로 설정하여 XML 컨버터를 완전히 제외
     * configureMessageConverters는 Spring Boot의 기본 컨버터 설정을 완전히 대체함
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 기본 컨버터들 추가 (XML 제외)
        converters.add(new ByteArrayHttpMessageConverter());

        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        stringConverter.setWriteAcceptCharset(false);
        converters.add(stringConverter);

        converters.add(new ResourceHttpMessageConverter());
        converters.add(new AllEncompassingFormHttpMessageConverter());

        // JSON 컨버터 - Spring Boot의 ObjectMapper 사용 (JSR-310 등 모듈 포함)
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter(objectMapper);
        converters.add(jsonConverter);

        // XML 컨버터는 의도적으로 추가하지 않음
        // jackson-dataformat-xml은 RestTemplate에서 공공 API XML 응답 파싱에만 사용
    }
}
