package com.example.wagemanager.domain.worker.dto;

import com.example.wagemanager.domain.worker.entity.Worker;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class WorkerDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long userId;
        private String workerCode;
        // private String accountNumber; // 현재 기획에는 노출되지 않음
        // private String bankName;      // 현재 기획에는 노출되지 않음
        private String kakaoPayLink;

        public static Response from(Worker worker) {
            return Response.builder()
                    .id(worker.getId())
                    .userId(worker.getUser().getId())
                    .workerCode(worker.getWorkerCode())
                    // .accountNumber(worker.getAccountNumber())
                    // .bankName(worker.getBankName())
                    .kakaoPayLink(worker.getKakaoPayLink())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        // private String accountNumber; // 현재 미사용
        // private String bankName;      // 현재 미사용
        private String kakaoPayLink;
    }
}
