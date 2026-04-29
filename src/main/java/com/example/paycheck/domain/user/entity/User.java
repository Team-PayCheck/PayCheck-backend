package com.example.paycheck.domain.user.entity;

import com.example.paycheck.common.BaseEntity;
import com.example.paycheck.domain.user.enums.UserType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kakao_id", unique = true, nullable = false)
    private String kakaoId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "phone")
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false)
    private UserType userType;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Version
    private Long version;

    public void updateProfile(String name, String phone, String profileImageUrl) {
        if (name != null) this.name = name;
        if (phone != null) this.phone = phone;
        if (profileImageUrl != null) this.profileImageUrl = profileImageUrl;
    }

    public void withdraw() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
