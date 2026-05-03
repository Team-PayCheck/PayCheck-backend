package com.example.paycheck.domain.user.service;

import com.example.paycheck.common.exception.BadRequestException;
import com.example.paycheck.common.exception.ErrorCode;
import com.example.paycheck.common.exception.FileUploadException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ProfileImageStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/heic",
            "image/heif"
    );
    private static final String DEFAULT_PROFILE_IMAGE_DIR = "profiles";

    private final S3Client s3Client;
    private final String bucket;
    private final String profileImageDir;

    public ProfileImageStorageService(
            S3Client s3Client,
            @Value("${aws.s3.bucket:}") String bucket,
            @Value("${aws.s3.profile-image-dir:profiles}") String profileImageDir) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.profileImageDir = normalizeProfileImageDir(profileImageDir);
    }

    public String uploadProfileImage(Long userId, MultipartFile file) {
        validateFile(file);
        validateBucketConfiguration();

        String contentType = file.getContentType();
        String objectKey = buildObjectKey(userId, file);

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, file.getSize()));

            URL uploadedFileUrl = s3Client.utilities()
                    .getUrl(GetUrlRequest.builder().bucket(bucket).key(objectKey).build());

            return uploadedFileUrl.toExternalForm();
        } catch (IOException | SdkException e) {
            throw new FileUploadException(
                    ErrorCode.PROFILE_IMAGE_UPLOAD_FAILED,
                    "프로필 이미지 업로드에 실패했습니다.",
                    e
            );
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException(ErrorCode.INVALID_PROFILE_IMAGE_FILE, "업로드할 이미지 파일이 비어 있습니다.");
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException(
                    ErrorCode.INVALID_PROFILE_IMAGE_FILE,
                    "프로필 이미지는 JPG, PNG, GIF, WEBP, HEIC 형식만 업로드할 수 있습니다."
            );
        }
    }

    private void validateBucketConfiguration() {
        if (!StringUtils.hasText(bucket)) {
            throw new FileUploadException(
                    ErrorCode.PROFILE_IMAGE_UPLOAD_NOT_CONFIGURED,
                    "프로필 이미지 업로드를 위한 S3 버킷 설정이 필요합니다."
            );
        }
    }

    private String buildObjectKey(Long userId, MultipartFile file) {
        String extension = resolveExtension(file);
        String fileName = UUID.randomUUID() + (StringUtils.hasText(extension) ? "." + extension : "");
        return profileImageDir + "/" + userId + "/" + fileName;
    }

    private String resolveExtension(MultipartFile file) {
        String originalExtension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (StringUtils.hasText(originalExtension)) {
            return sanitizeExtension(originalExtension);
        }

        return switch (file.getContentType().toLowerCase(Locale.ROOT)) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            case "image/heic" -> "heic";
            case "image/heif" -> "heif";
            default -> "";
        };
    }

    private String sanitizeExtension(String extension) {
        String normalizedExtension = extension.toLowerCase(Locale.ROOT);
        return normalizedExtension.matches("[a-z0-9]+") ? normalizedExtension : "";
    }

    private String normalizeProfileImageDir(String profileImageDir) {
        if (!StringUtils.hasText(profileImageDir)) {
            return DEFAULT_PROFILE_IMAGE_DIR;
        }

        String normalizedProfileImageDir = profileImageDir.replaceAll("^/+", "").replaceAll("/+$", "");
        return StringUtils.hasText(normalizedProfileImageDir) ? normalizedProfileImageDir : DEFAULT_PROFILE_IMAGE_DIR;
    }
}
