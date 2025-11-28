package com.example.wagemanager.domain.workplace.entity;

import com.example.wagemanager.common.BaseEntity;
import com.example.wagemanager.domain.employer.entity.Employer;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "workplace")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Workplace extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employer_id", nullable = false)
    private Employer employer;

    @Column(name = "business_number", unique = true, nullable = false)
    private String businessNumber;

    @Column(name = "business_name", nullable = false)
    private String businessName;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address")
    private String address;

    @Column(name = "color_code", length = 7)
    private String colorCode;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_less_than_five_employees", nullable = false)
    @Builder.Default
    private Boolean isLessThanFiveEmployees = true; // 5인 미만 여부 (고용주 선택)

    public void update(String businessName, String name, String address, String colorCode, Boolean isLessThanFiveEmployees) {
        if (businessName != null) this.businessName = businessName;
        if (name != null) this.name = name;
        if (address != null) this.address = address;
        if (colorCode != null) this.colorCode = colorCode;
        if (isLessThanFiveEmployees != null) this.isLessThanFiveEmployees = isLessThanFiveEmployees;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }
}
