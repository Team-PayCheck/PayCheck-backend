package com.example.wagemanager.global.security;

import com.example.wagemanager.domain.user.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring Security의 UserDetails 구현체
 * User 엔티티 정보를 Spring Security가 이해할 수 있는 형태로 변환
 */
@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // UserType을 권한으로 변환 (EMPLOYER, WORKER)
        return Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getUserType().name())
        );
    }

    @Override
    public String getPassword() {
        // 카카오 로그인이므로 비밀번호 없음
        return "";
    }

    @Override
    public String getUsername() {
        // userId를 username으로 사용
        return String.valueOf(user.getId());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
