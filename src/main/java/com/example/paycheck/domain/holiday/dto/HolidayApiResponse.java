package com.example.paycheck.domain.holiday.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.util.List;

/**
 * 한국천문연구원 특일정보 API 응답 DTO
 * XML 형식 응답 파싱용
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "response")
public class HolidayApiResponse {

    @JacksonXmlProperty(localName = "header")
    private Header header;

    @JacksonXmlProperty(localName = "body")
    private Body body;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {
        @JacksonXmlProperty(localName = "resultCode")
        private String resultCode;

        @JacksonXmlProperty(localName = "resultMsg")
        private String resultMsg;

        public boolean isSuccess() {
            return "00".equals(resultCode);
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        @JacksonXmlProperty(localName = "totalCount")
        private Integer totalCount;

        @JacksonXmlProperty(localName = "numOfRows")
        private Integer numOfRows;

        @JacksonXmlProperty(localName = "pageNo")
        private Integer pageNo;

        @JacksonXmlProperty(localName = "items")
        private Items items;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Items {
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "item")
        private List<HolidayItem> item;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HolidayItem {
        /**
         * 날짜 (예: "20250101")
         */
        @JacksonXmlProperty(localName = "locdate")
        private String locdate;

        /**
         * 공휴일명 (예: "신정")
         */
        @JacksonXmlProperty(localName = "dateName")
        private String dateName;

        /**
         * 비고
         */
        @JacksonXmlProperty(localName = "remarks")
        private String remarks;

        /**
         * 순번
         */
        @JacksonXmlProperty(localName = "seq")
        private Integer seq;

        /**
         * 공공기관 휴일 여부 (Y/N)
         * Y: 공공기관 쉬는 날 (실제 휴일)
         * N: 기념일이지만 출근일 (예: 24절기, 식목일)
         */
        @JacksonXmlProperty(localName = "isHoliday")
        private String isHoliday;

        /**
         * 공공기관 휴일 여부를 boolean으로 반환
         * @return Y이면 true, 그 외 false
         */
        public boolean isPublicHoliday() {
            return "Y".equalsIgnoreCase(isHoliday);
        }
    }
}
