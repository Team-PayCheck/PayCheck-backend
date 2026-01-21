package com.example.paycheck.domain.holiday.service;

import com.example.paycheck.domain.holiday.dto.HolidayApiResponse;
import com.example.paycheck.domain.holiday.entity.Holiday;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 한국천문연구원 특일정보 API 클라이언트
 * XML 파싱 전용 RestTemplate 사용 (컨트롤러와 분리)
 */
@Component
@Slf4j
public class HolidayApiClient {

    private static final String API_BASE_URL = "http://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService";
    private static final String GET_REST_DE_INFO = "/getRestDeInfo"; // 국경일 정보
    private static final String GET_HOL_DE_INFO = "/getHoliDeInfo";   // 공휴일 정보
    private static final String GET_ANNIVERSARY_INFO = "/getAnniversaryInfo"; // 기념일 정보
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Value("${holiday.api.key:}")
    private String apiKey;

    private final RestTemplate xmlRestTemplate;

    public HolidayApiClient(@Qualifier("xmlRestTemplate") RestTemplate xmlRestTemplate) {
        this.xmlRestTemplate = xmlRestTemplate;
    }

    /**
     * 특정 연도의 공휴일 정보 가져오기
     * 국경일 + 공휴일 정보를 모두 가져옴 (중복 제거)
     */
    public List<Holiday> fetchHolidays(int year) {
        // LinkedHashMap으로 날짜별 중복 제거 (순서 유지)
        Map<LocalDate, Holiday> holidayMap = new LinkedHashMap<>();

        try {
            // 1. 국경일 정보 조회 (신정, 삼일절, 광복절, 개천절, 한글날)
            List<Holiday> restDays = fetchRestDeInfo(year);
            for (Holiday holiday : restDays) {
                holidayMap.put(holiday.getHolidayDate(), holiday);
            }
            log.info("{}년 국경일 {}개 조회 완료", year, restDays.size());

            // 2. 공휴일 정보 조회 (설날, 석가탄신일, 어린이날, 현충일, 추석, 성탄절, 대체공휴일)
            List<Holiday> holDays = fetchHoliDeInfo(year);
            for (Holiday holiday : holDays) {
                // 중복된 날짜는 나중에 조회된 것으로 덮어쓰기 (공휴일 정보가 더 정확할 가능성)
                holidayMap.put(holiday.getHolidayDate(), holiday);
            }
            log.info("{}년 공휴일 {}개 조회 완료", year, holDays.size());

            List<Holiday> holidays = new ArrayList<>(holidayMap.values());
            log.info("{}년 총 공휴일 {}개 조회 완료 (중복 제거 후)", year, holidays.size());
            return holidays;

        } catch (Exception e) {
            log.error("공휴일 API 호출 실패: year={}, error={}", year, e.getMessage(), e);
            throw new RuntimeException("공휴일 정보를 가져오는데 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 국경일 정보 조회
     */
    private List<Holiday> fetchRestDeInfo(int year) throws Exception {
        String url = buildApiUrl(GET_REST_DE_INFO, year);
        return callApiAndParse(url, year);
    }

    /**
     * 공휴일 정보 조회
     */
    private List<Holiday> fetchHoliDeInfo(int year) throws Exception {
        String url = buildApiUrl(GET_HOL_DE_INFO, year);
        return callApiAndParse(url, year);
    }

    /**
     * API URL 생성
     */
    private String buildApiUrl(String endpoint, int year) {
        return UriComponentsBuilder
                .fromUriString(API_BASE_URL + endpoint)
                .queryParam("serviceKey", apiKey)
                .queryParam("solYear", year)
                .queryParam("numOfRows", 100)
                .queryParam("pageNo", 1)
                .queryParam("_type", "xml") // XML 응답 명시
                .build(false) // 인코딩하지 않음 (serviceKey 이중 인코딩 방지)
                .toUriString();
    }

    /**
     * API 호출 및 파싱
     * XML 전용 RestTemplate을 사용하여 자동으로 XML을 파싱
     */
    private List<Holiday> callApiAndParse(String url, int year) throws Exception {
        log.debug("API 호출: {}", url);

        // API 호출 및 XML 자동 파싱 (xmlRestTemplate에 XmlMessageConverter가 등록되어 있음)
        HolidayApiResponse response = xmlRestTemplate.getForObject(url, HolidayApiResponse.class);

        if (response == null) {
            log.warn("API 응답이 비어있습니다: year={}", year);
            return List.of();
        }

        // 응답 검증
        if (response.getHeader() == null || !response.getHeader().isSuccess()) {
            String errorMsg = response.getHeader() != null ? response.getHeader().getResultMsg() : "Unknown error";
            log.error("API 호출 실패: resultCode={}, resultMsg={}",
                      response.getHeader() != null ? response.getHeader().getResultCode() : "null",
                      errorMsg);
            throw new RuntimeException("공휴일 API 호출 실패: " + errorMsg);
        }

        // 데이터 변환
        return convertToHolidays(response, year);
    }

    /**
     * API 응답을 Holiday 엔티티 리스트로 변환
     */
    private List<Holiday> convertToHolidays(HolidayApiResponse response, int year) {
        List<Holiday> holidays = new ArrayList<>();

        if (response.getBody() == null ||
            response.getBody().getItems() == null ||
            response.getBody().getItems().getItem() == null) {
            log.warn("공휴일 데이터가 없습니다: year={}", year);
            return holidays;
        }

        for (HolidayApiResponse.HolidayItem item : response.getBody().getItems().getItem()) {
            try {
                LocalDate date = LocalDate.parse(item.getLocdate(), DATE_FORMATTER);

                Holiday holiday = Holiday.builder()
                        .holidayDate(date)
                        .year(date.getYear())
                        .month(date.getMonthValue())
                        .holidayName(item.getDateName())
                        .type(determineHolidayType(item.getDateName()))
                        .originalDate(item.getLocdate())
                        .remarks(item.getRemarks())
                        .isPublicHoliday(item.isPublicHoliday())
                        .build();

                holidays.add(holiday);

            } catch (Exception e) {
                log.error("공휴일 데이터 변환 실패: item={}, error={}", item, e.getMessage());
            }
        }

        return holidays;
    }

    /**
     * 공휴일명으로 타입 결정
     */
    private Holiday.HolidayType determineHolidayType(String dateName) {
        if (dateName == null) {
            return Holiday.HolidayType.LEGAL_HOLIDAY;
        }

        // 대체공휴일
        if (dateName.contains("대체") || dateName.contains("휴일")) {
            return Holiday.HolidayType.SUBSTITUTE_HOLIDAY;
        }

        // 임시공휴일
        if (dateName.contains("임시")) {
            return Holiday.HolidayType.TEMPORARY_HOLIDAY;
        }

        // 기본값: 법정공휴일
        return Holiday.HolidayType.LEGAL_HOLIDAY;
    }

    /**
     * API 키 설정 여부 확인
     */
    public boolean isApiKeyConfigured() {
        return apiKey != null && !apiKey.isEmpty() && !apiKey.isBlank();
    }
}
