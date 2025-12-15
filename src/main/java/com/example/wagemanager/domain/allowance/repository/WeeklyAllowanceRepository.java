package com.example.wagemanager.domain.allowance.repository;

import com.example.wagemanager.domain.allowance.entity.WeeklyAllowance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeeklyAllowanceRepository extends JpaRepository<WeeklyAllowance, Long> {

    @Query("SELECT wa FROM WeeklyAllowance wa " +
            "JOIN FETCH wa.contract c " +
            "WHERE c.id = :contractId")
    List<WeeklyAllowance> findByContractId(@Param("contractId") Long contractId);

    /**
     * 특정 계약의 특정 년/월에 해당하는 WeeklyAllowance 조회 (createdAt 기준)
     */
    @Query("""
            SELECT wa FROM WeeklyAllowance wa
            WHERE wa.contract.id = :contractId
            AND FUNCTION('YEAR', wa.createdAt) = :year
            AND FUNCTION('MONTH', wa.createdAt) = :month
            """)
    List<WeeklyAllowance> findByContractIdAndYearMonth(
            @Param("contractId") Long contractId,
            @Param("year") Integer year,
            @Param("month") Integer month
    );

    /**
     * 특정 날짜가 속한 주(월요일~일요일)의 WeeklyAllowance 조회
     * 같은 주에 이미 생성된 WeeklyAllowance가 있으면 반환
     * WorkRecord의 workDate를 기준으로 주차를 계산
     */
    @Query("""
            SELECT wa FROM WeeklyAllowance wa
            JOIN wa.workRecords wr
            WHERE wa.contract.id = :contractId
            AND FUNCTION('YEAR', wr.workDate) = FUNCTION('YEAR', :targetDate)
            AND FUNCTION('WEEK', wr.workDate) = FUNCTION('WEEK', :targetDate)
            """)
    List<WeeklyAllowance> findAllByContractAndWeek(@Param("contractId") Long contractId, @Param("targetDate") LocalDate targetDate);

    default Optional<WeeklyAllowance> findByContractAndWeek(Long contractId, LocalDate targetDate) {
        List<WeeklyAllowance> results = findAllByContractAndWeek(contractId, targetDate);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}
