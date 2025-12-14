package com.example.wagemanager.domain.holiday.entity;

import com.example.wagemanager.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 공휴일 엔티티
 * 한국천문연구원 특일정보 API 데이터 저장
 */
@Entity
@Table(name = "holidays",
        uniqueConstraints = @UniqueConstraint(columnNames = {"holiday_date"}),
        indexes = {
                @Index(name = "idx_holiday_date", columnList = "holiday_date"),
                @Index(name = "idx_year_month", columnList = "year, month")
        })
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Holiday extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 공휴일 날짜
     */
    @Column(name = "holiday_date", nullable = false, unique = true)
    private LocalDate holidayDate;

    /**
     * 연도 (조회 성능 향상용)
     */
    @Column(nullable = false)
    private Integer year;

    /**
     * 월 (조회 성능 향상용)
     */
    @Column(nullable = false)
    private Integer month;

    /**
     * 공휴일명 (예: "신정", "설날", "추석", "대체공휴일")
     */
    @Column(name = "holiday_name", nullable = false)
    private String holidayName;

    /**
     * 공휴일 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HolidayType type;

    /**
     * API 원본 데이터 (locdate)
     * 예: "20250101"
     */
    @Column(name = "original_date")
    private String originalDate;

    /**
     * 비고
     */
    @Column(length = 500)
    private String remarks;

    /**
     * 공휴일 타입
     */
    public enum HolidayType {
        LEGAL_HOLIDAY("법정공휴일"),           // 신정, 설날, 삼일절 등
        SUBSTITUTE_HOLIDAY("대체공휴일"),      // 설날 대체휴일, 추석 대체휴일 등
        TEMPORARY_HOLIDAY("임시공휴일"),       // 정부 지정 임시공휴일
        MEMORIAL_DAY("기념일");                // 24절기 등 (선택사항)

        private final String description;

        HolidayType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 해당 날짜가 주말(토/일)인지 확인
     */
    public boolean isWeekend() {
        return holidayDate.getDayOfWeek().getValue() >= 6;
    }

    /**
     * 공휴일 정보 업데이트
     */
    public void update(String holidayName, HolidayType type, String remarks) {
        this.holidayName = holidayName;
        this.type = type;
        this.remarks = remarks;
    }
}
