package com.example.wagemanager.domain.contract.repository;

import com.example.wagemanager.domain.contract.entity.WorkerContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkerContractRepository extends JpaRepository<WorkerContract, Long> {
    @Query("SELECT c FROM WorkerContract c " +
            "JOIN FETCH c.worker w " +
            "JOIN FETCH c.workplace wp " +
            "WHERE w.id = :workerId")
    List<WorkerContract> findByWorkerId(@Param("workerId") Long workerId);

    @Query("SELECT c FROM WorkerContract c " +
            "JOIN FETCH c.worker w " +
            "JOIN FETCH c.workplace wp " +
            "WHERE wp.id = :workplaceId")
    List<WorkerContract> findByWorkplaceId(@Param("workplaceId") Long workplaceId);

    @Query("SELECT c FROM WorkerContract c " +
            "JOIN FETCH c.worker w " +
            "JOIN FETCH c.workplace wp " +
            "WHERE wp.id = :workplaceId AND c.isActive = :isActive")
    List<WorkerContract> findByWorkplaceIdAndIsActive(@Param("workplaceId") Long workplaceId, @Param("isActive") Boolean isActive);

    @Query("SELECT c FROM WorkerContract c " +
            "JOIN FETCH c.worker w " +
            "JOIN FETCH c.workplace wp " +
            "WHERE w.id = :workerId AND wp.id = :workplaceId")
    Optional<WorkerContract> findByWorkerIdAndWorkplaceId(@Param("workerId") Long workerId, @Param("workplaceId") Long workplaceId);

    Integer countByWorkplaceIdAndIsActive(Long workplaceId, Boolean isActive);
}
