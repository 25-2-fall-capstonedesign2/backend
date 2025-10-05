package com.capstone.backend.service;

import com.capstone.backend.entity.User;
import com.capstone.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.capstone.backend.config.UserDetailsConfig;

// JWT 토큰에서 username(phoneNumber)을 추출했을 때,
// 해당 사용자가 DB에 존재하는지 확인하는 서비스
@Service
@RequiredArgsConstructor
public class UsernameService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String phoneNumber) throws UsernameNotFoundException {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + phoneNumber));
        return new UserDetailsConfig(user);
    }
}