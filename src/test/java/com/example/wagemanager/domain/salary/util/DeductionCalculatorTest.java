package com.example.wagemanager.domain.salary.util;

import com.example.wagemanager.domain.salary.util.DeductionCalculator.PayrollDeductionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("DeductionCalculator 테스트")
class DeductionCalculatorTest {

    @Test
    @DisplayName("세금 계산 - 프리랜서")
    void calculate_Freelancer() {
        // given
        BigDecimal totalGrossPay = BigDecimal.valueOf(2000000);

        // when
        DeductionCalculator.TaxResult result = DeductionCalculator.calculate(totalGrossPay, PayrollDeductionType.FREELANCER);

        // then
        assertThat(result).isNotNull();
        assertThat(result.totalInsurance).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.incomeTax).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.localIncomeTax).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.totalDeduction).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("세금 계산 - 비정규직 공제없음")
    void calculate_PartTimeNone() {
        // given
        BigDecimal totalGrossPay = BigDecimal.valueOf(1000000);

        // when
        DeductionCalculator.TaxResult result = DeductionCalculator.calculate(totalGrossPay, PayrollDeductionType.PART_TIME_NONE);

        // then
        assertThat(result).isNotNull();
        assertThat(result.totalInsurance).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.incomeTax).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.localIncomeTax).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.totalDeduction).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("세금 계산 - 비정규직 세금만")
    void calculate_PartTimeTaxOnly() {
        // given
        BigDecimal totalGrossPay = BigDecimal.valueOf(1500000);

        // when
        DeductionCalculator.TaxResult result = DeductionCalculator.calculate(totalGrossPay, PayrollDeductionType.PART_TIME_TAX_ONLY);

        // then
        assertThat(result).isNotNull();
        assertThat(result.totalInsurance).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.incomeTax).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.localIncomeTax).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.totalDeduction).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("세금 계산 - 비정규직 세금+4대보험")
    void calculate_PartTimeTaxAndInsurance() {
        // given
        BigDecimal totalGrossPay = BigDecimal.valueOf(2500000);

        // when
        DeductionCalculator.TaxResult result = DeductionCalculator.calculate(totalGrossPay, PayrollDeductionType.PART_TIME_TAX_AND_INSURANCE);

        // then
        assertThat(result).isNotNull();
        assertThat(result.nationalPension).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.healthInsurance).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.longTermCare).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.employmentInsurance).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.totalInsurance).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.incomeTax).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.localIncomeTax).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.totalDeduction).isGreaterThan(BigDecimal.ZERO);
    }
}
