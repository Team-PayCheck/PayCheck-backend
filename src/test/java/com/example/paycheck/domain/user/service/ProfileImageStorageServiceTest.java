package com.example.paycheck.domain.user.service;

import com.example.paycheck.common.exception.BadRequestException;
import com.example.paycheck.common.exception.FileUploadException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("ProfileImageStorageService 테스트")
class ProfileImageStorageServiceTest {

    @Test
    @DisplayName("프로필 이미지를 S3에 업로드하고 URL을 반환한다")
    void uploadProfileImage_Success() throws Exception {
        S3Client s3Client = mock(S3Client.class);
        S3Utilities s3Utilities = mock(S3Utilities.class);
        ProfileImageStorageService service = new ProfileImageStorageService(s3Client, "test-bucket", "profiles");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "profile.png",
                "image/png",
                "image-content".getBytes()
        );

        when(s3Client.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().eTag("etag").build());
        when(s3Client.utilities()).thenReturn(s3Utilities);
        when(s3Utilities.getUrl(any(GetUrlRequest.class)))
                .thenReturn(new URL("https://test-bucket.s3.ap-northeast-2.amazonaws.com/profiles/1/profile.png"));

        String result = service.uploadProfileImage(1L, file);

        assertThat(result).isEqualTo("https://test-bucket.s3.ap-northeast-2.amazonaws.com/profiles/1/profile.png");
        verify(s3Client).putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class));
    }

    @Test
    @DisplayName("이미지가 아닌 파일은 업로드할 수 없다")
    void uploadProfileImage_Fail_InvalidContentType() {
        S3Client s3Client = mock(S3Client.class);
        ProfileImageStorageService service = new ProfileImageStorageService(s3Client, "test-bucket", "profiles");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "profile.txt",
                "text/plain",
                "not-image".getBytes()
        );

        assertThatThrownBy(() -> service.uploadProfileImage(1L, file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("프로필 이미지는");
        verifyNoInteractions(s3Client);
    }

    @Test
    @DisplayName("S3 버킷이 설정되지 않으면 업로드할 수 없다")
    void uploadProfileImage_Fail_MissingBucket() {
        S3Client s3Client = mock(S3Client.class);
        ProfileImageStorageService service = new ProfileImageStorageService(s3Client, "", "profiles");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "profile.png",
                "image/png",
                "image-content".getBytes()
        );

        assertThatThrownBy(() -> service.uploadProfileImage(1L, file))
                .isInstanceOf(FileUploadException.class)
                .hasMessageContaining("S3 버킷 설정");
        verifyNoInteractions(s3Client);
    }
}
