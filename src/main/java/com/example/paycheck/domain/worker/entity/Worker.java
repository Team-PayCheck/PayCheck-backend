package com.example.paycheck.domain.worker.entity;

import com.example.paycheck.common.BaseEntity;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.global.encryption.AccountNumberEncryptor;
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
    @Convert(converter = AccountNumberEncryptor.class)
    private String accountNumber;

    @Column(name = "bank_name")
    private String bankName;

    public void updateAccount(String accountNumber, String bankName) {
        if (accountNumber != null) this.accountNumber = accountNumber;
        if (bankName != null) this.bankName = bankName;
    }
}
