package com.example.paycheck.domain.notice.dto;

import com.example.paycheck.domain.notice.entity.Notice;
import com.example.paycheck.domain.notice.enums.NoticeCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class NoticeDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "NoticeCreateRequest")
    public static class CreateRequest {
        @NotNull(message = "카테고리는 필수입니다.")
        private NoticeCategory category;

        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 100, message = "제목은 100자 이하로 입력해주세요.")
        private String title;

        @NotBlank(message = "내용은 필수입니다.")
        @Size(max = 5000, message = "내용은 5000자 이하로 입력해주세요.")
        private String content;

        @NotNull(message = "만료 일시는 필수입니다.")
        private LocalDateTime expiresAt;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "NoticeUpdateRequest")
    public static class UpdateRequest {
        private NoticeCategory category;

        @Size(max = 100, message = "제목은 100자 이하로 입력해주세요.")
        private String title;

        @Size(max = 5000, message = "내용은 5000자 이하로 입력해주세요.")
        private String content;

        @NotNull(message = "만료 일시는 필수입니다.")
        private LocalDateTime expiresAt;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "NoticeListResponse")
    public static class ListResponse {
        private Long id;
        private NoticeCategory category;
        private String title;
        private String authorName;
        private LocalDateTime createdAt;

        public static ListResponse from(Notice notice) {
            return ListResponse.builder()
                    .id(notice.getId())
                    .category(notice.getCategory())
                    .title(notice.getTitle())
                    .authorName(notice.getAuthor().getName())
                    .createdAt(notice.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "NoticeResponse")
    public static class Response {
        private Long id;
        private Long workplaceId;
        private String workplaceName;
        private Long authorId;
        private String authorName;
        private NoticeCategory category;
        private String title;
        private String content;
        private LocalDateTime expiresAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(Notice notice) {
            return Response.builder()
                    .id(notice.getId())
                    .workplaceId(notice.getWorkplace().getId())
                    .workplaceName(notice.getWorkplace().getName())
                    .authorId(notice.getAuthor().getId())
                    .authorName(notice.getAuthor().getName())
                    .category(notice.getCategory())
                    .title(notice.getTitle())
                    .content(notice.getContent())
                    .expiresAt(notice.getExpiresAt())
                    .createdAt(notice.getCreatedAt())
                    .updatedAt(notice.getUpdatedAt())
                    .build();
        }
    }
}
