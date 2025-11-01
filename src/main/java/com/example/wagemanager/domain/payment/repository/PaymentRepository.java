package com.example.wagemanager.domain.payment.repository;

import com.example.wagemanager.domain.payment.entity.Payment;
import com.example.wagemanager.domain.payment.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findBySalaryId(Long salaryId);
    List<Payment> findByStatus(PaymentStatus status);
}
