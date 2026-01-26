package com.example.paycheck.domain.salary;

import com.example.paycheck.domain.allowance.entity.WeeklyAllowance;
import com.example.paycheck.domain.contract.entity.WorkerContract;
import com.example.paycheck.domain.workrecord.entity.WorkRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Payroll precision dataset tests")
class PayrollPrecisionDatasetTest {

    @Test
    @DisplayName("Dataset-based payroll rounding verification to 1-won precision")
    void verifyRoundingWithDataset() throws IOException {
        Dataset dataset = readDataset();

        for (WeeklyPaidLeaveCase testCase : dataset.weeklyPaidLeave) {
            WorkerContract contract = mock(WorkerContract.class);
            when(contract.getHourlyWage()).thenReturn(new BigDecimal(testCase.hourlyWage));

            WeeklyAllowance allowance = WeeklyAllowance.builder()
                    .contract(contract)
                    .weekStartDate(LocalDate.of(2024, 1, 1))
                    .weekEndDate(LocalDate.of(2024, 1, 7))
                    .totalWorkHours(new BigDecimal(testCase.totalWorkHours))
                    .build();

            allowance.calculateWeeklyPaidLeave();

            BigDecimal actualWon = roundToWon(allowance.getWeeklyPaidLeaveAmount());
            assertThat(actualWon)
                    .as(testCase.name)
                    .isEqualByComparingTo(testCase.expectedWon);
        }

        for (WorkRecordCase testCase : dataset.workRecord) {
            WorkerContract contract = mock(WorkerContract.class);
            when(contract.getHourlyWage()).thenReturn(new BigDecimal(testCase.hourlyWage));

            WorkRecord record = WorkRecord.builder()
                    .contract(contract)
                    .workDate(LocalDate.parse(testCase.workDate))
                    .startTime(LocalTime.parse(testCase.startTime))
                    .endTime(LocalTime.parse(testCase.endTime))
                    .breakMinutes(testCase.breakMinutes)
                    .build();

            record.calculateHoursWithHolidayInfo(testCase.isHoliday, testCase.isSmallWorkplace);
            record.calculateSalaryWithAllowanceRules(testCase.isSmallWorkplace);

            BigDecimal actualWon = roundToWon(record.getTotalSalary());
            assertThat(actualWon)
                    .as(testCase.name)
                    .isEqualByComparingTo(testCase.expectedWon);
        }
    }

    private Dataset readDataset() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream input = PayrollPrecisionDatasetTest.class.getResourceAsStream(
                "/datasets/payroll_precision_cases.json")) {
            if (input == null) {
                throw new IOException("Dataset not found: /datasets/payroll_precision_cases.json");
            }
            return mapper.readValue(input, Dataset.class);
        }
    }

    private BigDecimal roundToWon(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP);
    }

    static class Dataset {
        public List<WeeklyPaidLeaveCase> weeklyPaidLeave;
        public List<WorkRecordCase> workRecord;
    }

    static class WeeklyPaidLeaveCase {
        public String name;
        public String hourlyWage;
        public String totalWorkHours;
        public String expectedWon;
    }

    static class WorkRecordCase {
        public String name;
        public String hourlyWage;
        public String workDate;
        public String startTime;
        public String endTime;
        public int breakMinutes;
        public boolean isHoliday;
        public boolean isSmallWorkplace;
        public String expectedWon;
    }
}
