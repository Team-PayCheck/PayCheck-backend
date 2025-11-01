package com.example.wagemanager.domain.contract.repository;

import com.example.wagemanager.domain.contract.entity.WorkerContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkerContractRepository extends JpaRepository<WorkerContract, Long> {
    List<WorkerContract> findByWorkerId(Long workerId);
    List<WorkerContract> findByWorkplaceId(Long workplaceId);
    Optional<WorkerContract> findByWorkerIdAndWorkplaceId(Long workerId, Long workplaceId);
}
