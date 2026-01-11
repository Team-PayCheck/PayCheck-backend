package com.example.wagemanager.domain.worker.entity;

import com.example.wagemanager.common.BaseEntity;
import com.example.wagemanager.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "worker")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Worker extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "worker_code", unique = true, nullable = false, length = 6)
    private String workerCode;

    @Column(name = "account_number")
    private String accountNumber; // AES-256 암호화 필요

    @Column(name = "bank_name")
    private String bankName;

    public void updateAccount(String accountNumber, String bankName) {
        if (accountNumber != null) this.accountNumber = accountNumber;
        if (bankName != null) this.bankName = bankName;
    }
}
