package com.example.paycheck.domain.fcm.entity;

import com.example.paycheck.common.BaseEntity;
import com.example.paycheck.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fcm_token",
        indexes = {
                @Index(name = "idx_fcm_token_user_id", columnList = "user_id"),
                @Index(name = "idx_fcm_token_token", columnList = "token", unique = true)
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FcmToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "device_info")
    private String deviceInfo;

    public void turnOverTo(User newUser, String newDeviceInfo) {
        this.user = newUser;
        this.deviceInfo = newDeviceInfo;
    }
}
