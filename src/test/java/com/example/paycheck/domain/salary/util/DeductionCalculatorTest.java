package com.example.paycheck.domain.salary.util;

import com.example.paycheck.domain.salary.util.DeductionCalculator.PayrollDeductionType;
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
        assertThat(result.incomeTax).isEqualByComparingTo("60000");
        assertThat(result.localIncomeTax).isEqualByComparingTo("6000");
        assertThat(result.totalTax).isEqualByComparingTo("66000");
        assertThat(result.totalDeduction).isEqualByComparingTo("66000");
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
        assertThat(result.totalTax).isEqualByComparingTo(BigDecimal.ZERO);
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
        assertThat(result.incomeTax).isEqualByComparingTo("8920");
        assertThat(result.localIncomeTax).isEqualByComparingTo("892");
        assertThat(result.totalTax).isEqualByComparingTo("9812");
        assertThat(result.totalDeduction).isEqualByComparingTo("9812");
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
        assertThat(result.nationalPension).isEqualByComparingTo("112500");
        assertThat(result.healthInsurance).isEqualByComparingTo("88625");
        assertThat(result.longTermCare).isEqualByComparingTo("11476");
        assertThat(result.employmentInsurance).isEqualByComparingTo("22500");
        assertThat(result.totalInsurance).isEqualByComparingTo("235101");
        assertThat(result.incomeTax).isEqualByComparingTo("35600");
        assertThat(result.localIncomeTax).isEqualByComparingTo("3560");
        assertThat(result.totalTax).isEqualByComparingTo("39160");
        assertThat(result.totalDeduction).isEqualByComparingTo("274261");
    }

    @Test
    @DisplayName("세금 계산 - 1천만원 초과 구간 수식 적용")
    void calculate_PartTimeTaxOnly_AboveTenMillion() {
        // given
        BigDecimal totalGrossPay = BigDecimal.valueOf(12000000);

        // when
        DeductionCalculator.TaxResult result = DeductionCalculator.calculate(totalGrossPay, PayrollDeductionType.PART_TIME_TAX_ONLY);

        // then
        assertThat(result).isNotNull();
        assertThat(result.incomeTax).isEqualByComparingTo("2218400");
        assertThat(result.localIncomeTax).isEqualByComparingTo("221840");
        assertThat(result.totalTax).isEqualByComparingTo("2440240");
        assertThat(result.totalDeduction).isEqualByComparingTo("2440240");
    }
}
