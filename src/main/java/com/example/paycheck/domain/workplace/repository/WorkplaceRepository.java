package com.example.paycheck.domain.workplace.repository;

import com.example.paycheck.domain.workplace.entity.Workplace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkplaceRepository extends JpaRepository<Workplace, Long> {
    List<Workplace> findByEmployerId(Long employerId);
    List<Workplace> findByEmployerIdAndIsActive(Long employerId, Boolean isActive);

    /**
     * 영구 삭제용: 특정 고용주의 모든 사업장 일괄 삭제
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Workplace w WHERE w.employer.id = :employerId")
    void deleteAllByEmployerId(@Param("employerId") Long employerId);
}
