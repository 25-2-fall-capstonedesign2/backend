package com.capstone.backend.controller;

import com.capstone.backend.dto.LoginRequestDto;
import com.capstone.backend.dto.TokenDto;
import com.capstone.backend.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Collections;
import jakarta.validation.Valid; // 추가
import com.capstone.backend.dto.MessageBoxDto; // 추가
import com.capstone.backend.dto.SignupRequestDto; // 추가
import org.springframework.security.core.annotation.AuthenticationPrincipal; // 추가
import org.springframework.security.core.userdetails.UserDetails; // 추가
import org.springframework.web.bind.annotation.DeleteMapping; // 추가

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 명세서의 POST /api/auth/login 엔드포인트 구현 [cite: 124]
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto loginRequestDto, HttpServletResponse response) {
        try {
            TokenDto tokenDto = authService.login(loginRequestDto);

            // 명세서에 따라 httpOnly 쿠키 설정 [cite: 132]
            Cookie cookie = new Cookie("ai_call_token", tokenDto.getToken());
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(24 * 60 * 60); // 1일 (명세서의 7일과 다름, 필요시 7 * 24 * 60 * 60으로 변경)
            cookie.setSecure(true); // HTTPS 배포 환경에서는 이 옵션을 반드시 활성화해야 합니다.
            //cookie.setSameSite("Lax"); // 명세서 권장 SameSite 설정
            response.addCookie(cookie);

            // 명세서에 따라 응답 바디에도 토큰 포함 [cite: 133]
            return ResponseEntity.ok(tokenDto);

        } catch (IllegalArgumentException e) {
            // 명세서에 따라 인증 실패 시 401 Unauthorized 응답 및 통합된 오류 메시지 반환
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("message", "전화번호 또는 비밀번호가 올바르지 않습니다."));
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<MessageBoxDto> signup(@Valid @RequestBody SignupRequestDto signupRequestDto) {
        try {
            authService.signup(signupRequestDto);
            return ResponseEntity.ok(new MessageBoxDto("회원가입이 완료되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageBoxDto(e.getMessage()));
        }
    }

    @DeleteMapping("/withdraw")
    public ResponseEntity<MessageBoxDto> withdraw(@AuthenticationPrincipal UserDetails userDetails) {
        // @AuthenticationPrincipal 어노테이션을 통해 현재 로그인한 사용자의 정보를 가져옴
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageBoxDto("인증된 사용자가 아닙니다."));
        }
        authService.withdraw(userDetails.getUsername());
        return ResponseEntity.ok(new MessageBoxDto("회원탈퇴가 성공적으로 처리되었습니다."));
    }
}
