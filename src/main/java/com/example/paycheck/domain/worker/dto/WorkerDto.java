package com.example.paycheck.domain.worker.dto;

import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.worker.entity.Worker;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class WorkerDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "WorkerResponse")
    public static class Response {
        private Long id;
        private Long userId;
        private String name;
        private String phone;
        private String profileImageUrl;
        private String workerCode;
        private String accountNumber;
        private String bankName;

        public static Response from(Worker worker) {
            User user = worker.getUser();
            return Response.builder()
                    .id(worker.getId())
                    .userId(user != null ? user.getId() : null)
                    .name(user != null ? user.getName() : null)
                    .phone(user != null ? user.getPhone() : null)
                    .profileImageUrl(user != null ? user.getProfileImageUrl() : null)
                    .workerCode(worker.getWorkerCode())
                    .accountNumber(worker.getAccountNumber())
                    .bankName(worker.getBankName())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "WorkerUpdateRequest")
    public static class UpdateRequest {
        @NotBlank(message = "계좌번호는 필수입니다.")
        @Size(max = 50, message = "계좌번호는 50자 이하로 입력해주세요.")
        private String accountNumber;
        @NotBlank(message = "은행명은 필수입니다.")
        @Size(max = 50, message = "은행명은 50자 이하로 입력해주세요.")
        private String bankName;
    }
}
