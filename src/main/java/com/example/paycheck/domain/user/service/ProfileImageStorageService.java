package com.example.paycheck.domain.user.service;

import com.example.paycheck.common.exception.BadRequestException;
import com.example.paycheck.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ProfileImageStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private final Path profileImageDirectory;
    private final String publicUriPrefix;

    public ProfileImageStorageService(
            @Value("${app.upload.profile-image-dir:uploads/profile-images}") String profileImageDirectory,
            @Value("${app.upload.profile-image-uri-prefix:/uploads/profile-images/}") String publicUriPrefix) {
        this.profileImageDirectory = Paths.get(profileImageDirectory).toAbsolutePath().normalize();
        this.publicUriPrefix = normalizePublicUriPrefix(publicUriPrefix);
    }

    public String store(MultipartFile file) {
        validate(file);

        try {
            Files.createDirectories(profileImageDirectory);

            String extension = extractExtension(file.getOriginalFilename());
            String storedFileName = UUID.randomUUID() + "." + extension;
            Path targetPath = profileImageDirectory.resolve(storedFileName).normalize();

            file.transferTo(targetPath);
            return publicUriPrefix + storedFileName;
        } catch (IOException e) {
            throw new IllegalStateException("프로필 이미지를 저장할 수 없습니다.", e);
        }
    }

    public void deleteIfStoredLocally(String profileImageUrl) {
        if (!StringUtils.hasText(profileImageUrl) || !profileImageUrl.startsWith(publicUriPrefix)) {
            return;
        }

        String fileName = profileImageUrl.substring(publicUriPrefix.length());
        if (!StringUtils.hasText(fileName)) {
            return;
        }

        try {
            Files.deleteIfExists(profileImageDirectory.resolve(fileName).normalize());
        } catch (IOException ignored) {
            // 이전 프로필 이미지 정리에 실패해도 업로드 성공 흐름은 유지한다.
        }
    }

    public Path getProfileImageDirectory() {
        return profileImageDirectory;
    }

    public String getPublicUriPrefix() {
        return publicUriPrefix;
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException(ErrorCode.INVALID_INPUT_VALUE, "업로드할 프로필 이미지 파일이 필요합니다.");
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.startsWith("image/")) {
            throw new BadRequestException(ErrorCode.INVALID_INPUT_VALUE, "이미지 파일만 업로드할 수 있습니다.");
        }

        extractExtension(file.getOriginalFilename());
    }

    private String extractExtension(String originalFilename) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        if (!StringUtils.hasText(extension)) {
            throw new BadRequestException(ErrorCode.INVALID_INPUT_VALUE, "지원하지 않는 이미지 형식입니다.");
        }

        String normalizedExtension = extension.toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(normalizedExtension)) {
            throw new BadRequestException(ErrorCode.INVALID_INPUT_VALUE, "지원하지 않는 이미지 형식입니다.");
        }

        return normalizedExtension;
    }

    private String normalizePublicUriPrefix(String publicUriPrefix) {
        String normalized = StringUtils.hasText(publicUriPrefix) ? publicUriPrefix.trim() : "/uploads/profile-images/";
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }
        return normalized;
    }
}
