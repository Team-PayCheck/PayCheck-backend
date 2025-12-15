package com.example.wagemanager.domain.worker.repository;

import com.example.wagemanager.domain.worker.entity.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkerRepository extends JpaRepository<Worker, Long> {
    @Query("SELECT w FROM Worker w " +
            "JOIN FETCH w.user u " +
            "WHERE u.id = :userId")
    Optional<Worker> findByUserId(@Param("userId") Long userId);

    @Query("SELECT w FROM Worker w WHERE w.workerCode = :workerCode")
    Optional<Worker> findByWorkerCode(@Param("workerCode") String workerCode);

    boolean existsByWorkerCode(String workerCode);
}
