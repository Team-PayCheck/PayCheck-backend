package com.example.wagemanager.global.config;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

/**
 * 외부 API 호출 시 사용할 RestTemplate 설정
 * - 일반 RestTemplate: JSON 처리용
 * - XML RestTemplate: 공공 API XML 파싱용 (컨트롤러와 분리)
 *
 * 주의: XmlMapper를 Bean으로 등록하지 않음 (Spring Boot 자동 설정이
 * MappingJackson2XmlHttpMessageConverter를 등록하는 것을 방지)
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 일반 RestTemplate (JSON 처리용)
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 5초
        factory.setReadTimeout(5000); // 5초

        return builder
                .requestFactory(() -> factory)
                .build();
    }

    /**
     * XML 전용 RestTemplate
     * 공공 API XML 파싱용으로 별도 분리
     * 컨트롤러의 MessageConverter와 독립적으로 동작
     *
     * XmlMapper를 Bean으로 등록하지 않고 직접 생성하여 사용
     * (Bean으로 등록하면 Spring Boot가 자동으로 XML 컨버터를 MVC에 추가함)
     */
    @Bean
    public RestTemplate xmlRestTemplate(RestTemplateBuilder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 5초
        factory.setReadTimeout(5000); // 5초

        // XmlMapper를 직접 생성 (Bean으로 등록하지 않음)
        XmlMapper xmlMapper = new XmlMapper();

        return builder
                .requestFactory(() -> factory)
                .additionalMessageConverters(new MappingJackson2XmlHttpMessageConverter(xmlMapper))
                .build();
    }
}
