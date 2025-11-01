package com.example.wagemanager.domain.worker.repository;

import com.example.wagemanager.domain.worker.entity.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkerRepository extends JpaRepository<Worker, Long> {
    Optional<Worker> findByUserId(Long userId);
    Optional<Worker> findByWorkerCode(String workerCode);
    boolean existsByWorkerCode(String workerCode);
}
