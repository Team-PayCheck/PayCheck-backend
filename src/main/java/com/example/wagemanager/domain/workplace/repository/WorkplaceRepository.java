package com.example.wagemanager.domain.workplace.repository;

import com.example.wagemanager.domain.workplace.entity.Workplace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkplaceRepository extends JpaRepository<Workplace, Long> {
    List<Workplace> findByEmployerId(Long employerId);
}
