package com.example.paycheck.domain.workplace.service;

import com.example.paycheck.domain.workplace.dto.NtsBusinessStatusRequest;
import com.example.paycheck.domain.workplace.dto.NtsBusinessStatusResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Slf4j
public class NtsBusinessStatusClient {

    private final RestTemplate restTemplate;

    @Value("${nts.business-status.base-url:}")
    private String baseUrl;

    @Value("${nts.business-status.status-path:}")
    private String statusPath;

    @Value("${nts.business-status.service-key:}")
    private String serviceKey;

    public NtsBusinessStatusClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public NtsBusinessStatusResponse fetchStatus(NtsBusinessStatusRequest request) {
        String url = buildStatusUrl();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<NtsBusinessStatusRequest> entity = new HttpEntity<>(request, headers);

        try {
            return restTemplate.postForObject(url, entity, NtsBusinessStatusResponse.class);
        } catch (RestClientException e) {
            log.error("국세청 사업자등록 상태조회 API 호출 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    public boolean isConfigured() {
        return hasText(baseUrl) && hasText(statusPath) && hasText(serviceKey);
    }

    private String buildStatusUrl() {
        return UriComponentsBuilder
                .fromUriString(baseUrl)
                .path(statusPath)
                .queryParam("serviceKey", serviceKey)
                .build(false)
                .toUriString();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
