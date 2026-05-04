package com.example.paycheck.domain.user.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
public class ProfileImageUrlResolver {

    public String resolve(String profileImageUrl) {
        if (!StringUtils.hasText(profileImageUrl)) {
            return profileImageUrl;
        }

        if (profileImageUrl.startsWith("http://") || profileImageUrl.startsWith("https://")) {
            return profileImageUrl;
        }

        if (!profileImageUrl.startsWith("/")) {
            return profileImageUrl;
        }

        try {
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path(profileImageUrl)
                    .toUriString();
        } catch (IllegalStateException e) {
            return profileImageUrl;
        }
    }
}
