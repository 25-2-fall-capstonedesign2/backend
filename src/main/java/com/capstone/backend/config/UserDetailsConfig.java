package com.capstone.backend.config;

import com.capstone.backend.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.Collections;

// Spring Security가 사용자를 인증할 때 사용하는 UserDetails 객체
public class UserDetailsConfig implements UserDetails {

    private final User user;

    public UserDetailsConfig(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getPhoneNumber();
    }

    // 아래 메소드들은 계정 상태 관련 설정 (지금은 모두 true로 설정)
    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return true; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList(); // 지금은 권한(Role)을 사용하지 않음
    }
}
