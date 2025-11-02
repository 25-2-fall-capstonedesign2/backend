package com.capstone.backend.service;

import com.capstone.backend.dto.LoginRequestDto;
import com.capstone.backend.dto.TokenDto;
import com.capstone.backend.entity.User;
import com.capstone.backend.repository.UserRepository;
import com.capstone.backend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.capstone.backend.dto.SignupRequestDto;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public TokenDto login(LoginRequestDto loginRequestDto) {
        // 1. 전화번호 기반으로 사용자 조회 [cite: 113]
        User user = userRepository.findByPhoneNumber(loginRequestDto.getPhone())
                .orElseThrow(() -> new IllegalArgumentException("AUTH_ERROR")); // 사용자가 없어도 동일한 에러 메시지 반환

        // 2. 비밀번호 일치 여부 확인 (bcrypt)
        if (!passwordEncoder.matches(loginRequestDto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("AUTH_ERROR"); // 비밀번호가 틀려도 동일한 에러 메시지 반환
        }

        // 3. JWT 토큰 생성 [cite: 115]
        String token = jwtUtil.generateToken(user.getPhoneNumber());
        return new TokenDto(token);
    }

    @Transactional
    public void signup(SignupRequestDto signupRequestDto) {
        // 전화번호 중복 확인
        if (userRepository.findByPhoneNumber(signupRequestDto.getPhone()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 전화번호입니다.");
        }

        // 사용자 정보 생성 및 저장
        User user = new User(signupRequestDto.getPhone(),
                passwordEncoder.encode(signupRequestDto.getPassword()),
                signupRequestDto.getDisplayName());
        userRepository.save(user);
    }

    @Transactional
    public void withdraw(String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        userRepository.delete(user);
    }
}
