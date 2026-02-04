package com.example.paycheck.domain.salary.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 급여 공제 계산 유틸리티
 *
 * 대상자:
 * - 프리랜서: 소득세 3% + 지방소득세 0.3%
 * - 비정규직(알바): 세금/보험료 적용 여부에 따라 계산
 */
public class DeductionCalculator {
    private static final int DEFAULT_FAMILY_COUNT = 1;
    private static final int MIN_TABLE_SALARY_THOUSAND = 770;
    private static final int MAX_TABLE_SALARY_THOUSAND = 10000;
    private static final String INCOME_TAX_TABLE_RESOURCE = "tax/income_tax_table_2024.json";

    /**
     * 급여 공제 유형
     * 프리랜서와 비정규직의 세금 및 4대보험 공제 방식을 통합 관리
     */
    public enum PayrollDeductionType {
        FREELANCER,                     // 프리랜서: 소득세 3% + 지방소득세 0.3%
        PART_TIME_NONE,                 // 비정규직: 세금 X, 4대보험 X
        PART_TIME_TAX_ONLY,             // 비정규직: 세금 O, 4대보험 X
        PART_TIME_TAX_AND_INSURANCE     // 비정규직: 세금 O, 4대보험 O
    }

    // 최소 국민연금 보험료 기준 월급 (39만원)
    private static final BigDecimal MINIMUM_PENSION_WAGE = new BigDecimal("390000");

    // 4대보험 요율
    private static final BigDecimal NATIONAL_PENSION_RATE = new BigDecimal("0.045");     // 국민연금 4.5%
    private static final BigDecimal HEALTH_INSURANCE_RATE = new BigDecimal("0.03545");  // 건강보험 3.545%
    private static final BigDecimal LONG_TERM_CARE_RATE = new BigDecimal("0.1295");    // 장기요양보험 (건강보험의 12.95%)
    private static final BigDecimal EMPLOYMENT_INSURANCE_RATE = new BigDecimal("0.009"); // 고용보험 0.9%

    /**
     * 통합 세금 및 보험료 계산 결과
     * 프리랜서와 비정규직 모두 사용 가능한 통합 결과 클래스
     */
    public static class TaxResult {
        public BigDecimal nationalPension;     // 국민연금
        public BigDecimal healthInsurance;     // 건강보험
        public BigDecimal longTermCare;        // 장기요양보험
        public BigDecimal employmentInsurance; // 고용보험
        public BigDecimal totalInsurance;      // 총 보험료

        public BigDecimal incomeTax;           // 소득세
        public BigDecimal localIncomeTax;      // 지방소득세
        public BigDecimal totalTax;            // 총 세금

        public BigDecimal totalDeduction;      // 총 공제 (보험료 + 세금)

        public TaxResult(BigDecimal grossPay, PayrollDeductionType deductionType) {
            switch (deductionType) {
                case FREELANCER:
                    // 프리랜서: 소득세 3% + 지방소득세 0.3%
                    this.nationalPension = BigDecimal.ZERO;
                    this.healthInsurance = BigDecimal.ZERO;
                    this.longTermCare = BigDecimal.ZERO;
                    this.employmentInsurance = BigDecimal.ZERO;
                    this.totalInsurance = BigDecimal.ZERO;

                    this.incomeTax = grossPay.multiply(new BigDecimal("0.03"))
                        .setScale(0, RoundingMode.DOWN);
                    this.localIncomeTax = grossPay.multiply(new BigDecimal("0.003"))
                        .setScale(0, RoundingMode.DOWN);
                    this.totalTax = this.incomeTax.add(this.localIncomeTax);
                    this.totalDeduction = this.totalTax;
                    break;

                case PART_TIME_NONE:
                    // 비정규직: 세금 X, 4대보험 X
                    this.nationalPension = BigDecimal.ZERO;
                    this.healthInsurance = BigDecimal.ZERO;
                    this.longTermCare = BigDecimal.ZERO;
                    this.employmentInsurance = BigDecimal.ZERO;
                    this.totalInsurance = BigDecimal.ZERO;
                    this.incomeTax = BigDecimal.ZERO;
                    this.localIncomeTax = BigDecimal.ZERO;
                    this.totalTax = BigDecimal.ZERO;
                    this.totalDeduction = BigDecimal.ZERO;
                    break;

                case PART_TIME_TAX_ONLY:
                    // 비정규직: 세금 O, 4대보험 X
                    this.nationalPension = BigDecimal.ZERO;
                    this.healthInsurance = BigDecimal.ZERO;
                    this.longTermCare = BigDecimal.ZERO;
                    this.employmentInsurance = BigDecimal.ZERO;
                    this.totalInsurance = BigDecimal.ZERO;

                    this.incomeTax = calculateSimpleIncomeTax(grossPay)
                        .setScale(0, RoundingMode.DOWN);
                    this.localIncomeTax = this.incomeTax.multiply(new BigDecimal("0.1"))
                        .setScale(0, RoundingMode.DOWN);
                    this.totalTax = this.incomeTax.add(this.localIncomeTax);
                    this.totalDeduction = this.totalTax;
                    break;

                case PART_TIME_TAX_AND_INSURANCE:
                    // 비정규직: 세금 O, 4대보험 O
                    BigDecimal pensionBaseSalary = grossPay.compareTo(MINIMUM_PENSION_WAGE) < 0
                        ? MINIMUM_PENSION_WAGE
                        : grossPay;
                    pensionBaseSalary = pensionBaseSalary.setScale(-3, RoundingMode.DOWN);

                    this.nationalPension = pensionBaseSalary.multiply(NATIONAL_PENSION_RATE)
                        .setScale(0, RoundingMode.DOWN);
                    this.healthInsurance = grossPay.multiply(HEALTH_INSURANCE_RATE)
                        .setScale(0, RoundingMode.DOWN);
                    this.longTermCare = this.healthInsurance.multiply(LONG_TERM_CARE_RATE)
                        .setScale(0, RoundingMode.DOWN);
                    this.employmentInsurance = grossPay.multiply(EMPLOYMENT_INSURANCE_RATE)
                        .setScale(0, RoundingMode.DOWN);
                    this.totalInsurance = this.nationalPension.add(this.healthInsurance)
                        .add(this.longTermCare).add(this.employmentInsurance);

                    this.incomeTax = calculateSimpleIncomeTax(grossPay)
                        .setScale(0, RoundingMode.DOWN);
                    this.localIncomeTax = this.incomeTax.multiply(new BigDecimal("0.1"))
                        .setScale(0, RoundingMode.DOWN);
                    this.totalTax = this.incomeTax.add(this.localIncomeTax);
                    this.totalDeduction = this.totalInsurance.add(this.totalTax);
                    break;
            }
        }
    }

    /**
     * 통합 세금 및 보험료 계산
     */
    public static TaxResult calculate(BigDecimal grossPay, PayrollDeductionType deductionType) {
        return new TaxResult(grossPay, deductionType);
    }

    /**
     * 간이세액표를 반영한 세금 계산
     */
    private static BigDecimal calculateSimpleIncomeTax(BigDecimal grossPay) {
        return calculateIncomeTaxFromTable(grossPay, DEFAULT_FAMILY_COUNT);
    }

    private static BigDecimal calculateIncomeTaxFromTable(BigDecimal grossPay, int familyCount) {
        if (grossPay == null || grossPay.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        int normalizedFamilyCount = Math.max(1, Math.min(11, familyCount));
        BigDecimal thousand = grossPay.divide(new BigDecimal("1000"), 0, RoundingMode.DOWN);
        int salaryThousand = thousand.intValue();

        if (salaryThousand < MIN_TABLE_SALARY_THOUSAND) {
            return BigDecimal.ZERO;
        }

        TaxTable table = TaxTableHolder.TABLE;
        BigDecimal tableTax = table.lookup(salaryThousand, normalizedFamilyCount);
        if (tableTax != null) {
            return tableTax;
        }

        if (salaryThousand <= MAX_TABLE_SALARY_THOUSAND) {
            return BigDecimal.ZERO;
        }

        return calculateFormulaTax(grossPay, normalizedFamilyCount, table);
    }

    private static BigDecimal calculateFormulaTax(BigDecimal grossPay, int familyCount, TaxTable table) {
        BigDecimal baseTax = table.lookup(MAX_TABLE_SALARY_THOUSAND, familyCount);
        if (baseTax == null) {
            baseTax = BigDecimal.ZERO;
        }

        BigDecimal grossPayWon = grossPay.setScale(0, RoundingMode.DOWN);
        // 간이세액표 하단 수식 구간 경계(단위: 원)
        BigDecimal tenMillion = new BigDecimal("10000000");
        BigDecimal fourteenMillion = new BigDecimal("14000000");
        BigDecimal twentyEightMillion = new BigDecimal("28000000");
        BigDecimal thirtyMillion = new BigDecimal("30000000");
        BigDecimal fortyFiveMillion = new BigDecimal("45000000");
        BigDecimal eightySevenMillion = new BigDecimal("87000000");

        if (grossPayWon.compareTo(fourteenMillion) <= 0) {
            BigDecimal over = grossPayWon.subtract(tenMillion).max(BigDecimal.ZERO);
            return baseTax
                .add(over.multiply(new BigDecimal("0.98")).multiply(new BigDecimal("0.35")))
                .add(new BigDecimal("25000"));
        }
        if (grossPayWon.compareTo(twentyEightMillion) <= 0) {
            BigDecimal over = grossPayWon.subtract(fourteenMillion).max(BigDecimal.ZERO);
            return baseTax
                .add(new BigDecimal("1397000"))
                .add(over.multiply(new BigDecimal("0.98")).multiply(new BigDecimal("0.38")));
        }
        if (grossPayWon.compareTo(thirtyMillion) <= 0) {
            BigDecimal over = grossPayWon.subtract(twentyEightMillion).max(BigDecimal.ZERO);
            return baseTax
                .add(new BigDecimal("6610600"))
                .add(over.multiply(new BigDecimal("0.98")).multiply(new BigDecimal("0.40")));
        }
        if (grossPayWon.compareTo(fortyFiveMillion) <= 0) {
            BigDecimal over = grossPayWon.subtract(thirtyMillion).max(BigDecimal.ZERO);
            return baseTax
                .add(new BigDecimal("7394600"))
                .add(over.multiply(new BigDecimal("0.40")));
        }
        if (grossPayWon.compareTo(eightySevenMillion) <= 0) {
            BigDecimal over = grossPayWon.subtract(fortyFiveMillion).max(BigDecimal.ZERO);
            return baseTax
                .add(new BigDecimal("13394600"))
                .add(over.multiply(new BigDecimal("0.42")));
        }
        BigDecimal over = grossPayWon.subtract(eightySevenMillion).max(BigDecimal.ZERO);
        return baseTax
            .add(new BigDecimal("31034600"))
            .add(over.multiply(new BigDecimal("0.45")));
    }

    private static class TaxTableHolder {
        private static final TaxTable TABLE = loadTaxTable();
    }

    private static TaxTable loadTaxTable() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try (InputStream stream = DeductionCalculator.class.getClassLoader()
            .getResourceAsStream(INCOME_TAX_TABLE_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Income tax table resource not found: " + INCOME_TAX_TABLE_RESOURCE);
            }
            TaxTableData data = mapper.readValue(stream, TaxTableData.class);
            return TaxTable.fromRows(data.rows);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load income tax table", e);
        }
    }

    private static class TaxTableData {
        public List<TaxTableRow> rows;
    }

    private static class TaxTableRow {
        public int min_salary_thousand;
        public int max_salary_thousand;
        public int family_count;
        public int income_tax_won;
    }

    private static class TaxTable {
        private final Map<Integer, List<TaxRange>> rangesByFamily;

        private TaxTable(Map<Integer, List<TaxRange>> rangesByFamily) {
            this.rangesByFamily = rangesByFamily;
        }

        private static TaxTable fromRows(List<TaxTableRow> rows) {
            Map<Integer, List<TaxRange>> map = new HashMap<>();
            if (rows != null) {
                for (TaxTableRow row : rows) {
                    map.computeIfAbsent(row.family_count, key -> new ArrayList<>())
                        .add(new TaxRange(row.min_salary_thousand, row.max_salary_thousand, row.income_tax_won));
                }
            }
            return new TaxTable(map);
        }

        private BigDecimal lookup(int salaryThousand, int familyCount) {
            List<TaxRange> ranges = rangesByFamily.get(familyCount);
            if (ranges == null) {
                return null;
            }
            for (TaxRange range : ranges) {
                if (range.matches(salaryThousand)) {
                    return new BigDecimal(range.incomeTaxWon);
                }
            }
            return null;
        }
    }

    private static class TaxRange {
        private final int min;
        private final int max;
        private final int incomeTaxWon;

        private TaxRange(int min, int max, int incomeTaxWon) {
            this.min = min;
            this.max = max;
            this.incomeTaxWon = incomeTaxWon;
        }

        private boolean matches(int salaryThousand) {
            if (min == max) {
                return salaryThousand == min;
            }
            return salaryThousand >= min && salaryThousand < max;
        }
    }
}
