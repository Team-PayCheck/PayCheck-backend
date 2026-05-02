package com.example.paycheck.domain.user.service;

import com.example.paycheck.common.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ProfileImageStorageService 테스트")
class ProfileImageStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("프로필 이미지를 저장하고 공개 경로를 반환한다")
    void store_Success() throws Exception {
        ProfileImageStorageService storageService = new ProfileImageStorageService(
                tempDir.resolve("profile-images").toString(),
                "/uploads/profile-images/"
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "image-bytes".getBytes()
        );

        String storedPath = storageService.store(file);

        assertThat(storedPath).startsWith("/uploads/profile-images/");
        try (var storedFiles = Files.list(storageService.getProfileImageDirectory())) {
            assertThat(storedFiles.count()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("이미지가 아닌 파일은 업로드할 수 없다")
    void store_Fail_InvalidContentType() {
        ProfileImageStorageService storageService = new ProfileImageStorageService(
                tempDir.resolve("profile-images").toString(),
                "/uploads/profile-images/"
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.txt",
                "text/plain",
                "not-image".getBytes()
        );

        assertThatThrownBy(() -> storageService.store(file))
                .isInstanceOf(BadRequestException.class);
    }
}
