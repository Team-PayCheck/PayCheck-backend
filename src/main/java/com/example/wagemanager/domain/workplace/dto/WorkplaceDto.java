package com.example.wagemanager.domain.workplace.dto;

import com.example.wagemanager.domain.workplace.entity.Workplace;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class WorkplaceDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "사업자 등록번호는 필수입니다.")
        @Pattern(regexp = "\\d{3}-\\d{2}-\\d{5}", message = "사업자 등록번호 형식이 올바르지 않습니다. (예: 123-45-67890)")
        private String businessNumber;

        @NotBlank(message = "사업장명은 필수입니다.")
        private String businessName;

        @NotBlank(message = "지점명은 필수입니다.")
        private String name;

        private String address;

        @Pattern(regexp = "^#([A-Fa-f0-9]{6})$", message = "색상 코드 형식이 올바르지 않습니다. (예: #FF5733)")
        private String colorCode;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String businessName;
        private String name;
        private String address;

        @Pattern(regexp = "^#([A-Fa-f0-9]{6})$", message = "색상 코드 형식이 올바르지 않습니다. (예: #FF5733)")
        private String colorCode;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String businessNumber;
        private String businessName;
        private String name;
        private String address;
        private String colorCode;
        private Boolean isActive;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(Workplace workplace) {
            return Response.builder()
                    .id(workplace.getId())
                    .businessNumber(workplace.getBusinessNumber())
                    .businessName(workplace.getBusinessName())
                    .name(workplace.getName())
                    .address(workplace.getAddress())
                    .colorCode(workplace.getColorCode())
                    .isActive(workplace.getIsActive())
                    .createdAt(workplace.getCreatedAt())
                    .updatedAt(workplace.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResponse {
        private Long id;
        private String businessName;
        private String name;
        private String colorCode;
        private Integer workerCount;
        private Boolean isActive;

        public static ListResponse from(Workplace workplace, Integer workerCount) {
            return ListResponse.builder()
                    .id(workplace.getId())
                    .businessName(workplace.getBusinessName())
                    .name(workplace.getName())
                    .colorCode(workplace.getColorCode())
                    .workerCount(workerCount)
                    .isActive(workplace.getIsActive())
                    .build();
        }
    }
}
