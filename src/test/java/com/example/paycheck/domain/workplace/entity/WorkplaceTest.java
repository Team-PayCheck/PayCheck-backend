package com.example.paycheck.domain.workplace.entity;

import com.example.paycheck.domain.employer.entity.Employer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Workplace 엔티티 테스트")
class WorkplaceTest {

    private Workplace workplace;
    private Employer mockEmployer;

    @BeforeEach
    void setUp() {
        mockEmployer = mock(Employer.class);
        workplace = Workplace.builder()
                .id(1L)
                .employer(mockEmployer)
                .businessNumber("123-45-67890")
                .name("본점")
                .address("서울시 강남구")
                .colorCode("#FF5733")
                .isActive(true)
                .isLessThanFiveEmployees(true)
                .build();
    }

    @Test
    @DisplayName("사업장 정보 업데이트 - 모든 필드")
    void update_AllFields() {
        // when
        workplace.update(
                "지점",
                "서울시 서초구",
                "#33FF57",
                false
        );

        // then
        assertThat(workplace.getName()).isEqualTo("지점");
        assertThat(workplace.getAddress()).isEqualTo("서울시 서초구");
        assertThat(workplace.getColorCode()).isEqualTo("#33FF57");
        assertThat(workplace.getIsLessThanFiveEmployees()).isFalse();
    }

    @Test
    @DisplayName("사업장 정보 업데이트 - 일부 필드만")
    void update_PartialFields() {
        // when
        workplace.update(
                "수정된 근무지명",
                null,
                null,
                null
        );

        // then
        assertThat(workplace.getName()).isEqualTo("수정된 근무지명");
        assertThat(workplace.getAddress()).isEqualTo("서울시 강남구"); // unchanged
    }

    @Test
    @DisplayName("사업장 정보 업데이트 - null 값은 변경되지 않음")
    void update_NullValues() {
        // given
        String originalName = workplace.getName();

        // when
        workplace.update(null, null, null, null);

        // then
        assertThat(workplace.getName()).isEqualTo(originalName);
    }

    @Test
    @DisplayName("사업장 비활성화")
    void deactivate() {
        // when
        workplace.deactivate();

        // then
        assertThat(workplace.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("사업장 활성화")
    void activate() {
        // given
        workplace.deactivate();

        // when
        workplace.activate();

        // then
        assertThat(workplace.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("기본값 확인")
    void defaultValues() {
        // given
        Workplace defaultWorkplace = Workplace.builder()
                .employer(mockEmployer)
                .businessNumber("999-99-99999")
                .name("기본")
                .build();

        // then
        assertThat(defaultWorkplace.getIsActive()).isTrue();
        assertThat(defaultWorkplace.getIsLessThanFiveEmployees()).isTrue();
    }
}
