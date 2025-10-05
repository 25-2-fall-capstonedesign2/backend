package com.capstone.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.capstone.backend.entity.User;
import com.capstone.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc // MockMvc를 주입받아 사용하기 위한 어노테이션
@Transactional // 각 테스트 케이스 실행 후 DB 상태를 롤백하여 독립성 보장
class BackendApplicationTests {
    // MockMvc: 실제 서버를 띄우지 않고 API를 테스트할 수 있게 해주는 도구
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Java 객체를 JSON 문자열로, 또는 그 반대로 변환해주는 도구
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 각 테스트가 실행되기 전에 항상 먼저 실행되는 메소드.
     * 테스트에 필요한 데이터를 미리 H2 DB에 준비합니다.
     */
    @BeforeEach
    void setUp() {
        userRepository.deleteAll(); // 이전 테스트 데이터를 깔끔하게 지웁니다.
        User testUser = new User();
        testUser.setPhoneNumber("01012345678");
        testUser.setPassword(passwordEncoder.encode("password1234"));
        testUser.setDisplayName("테스트유저");
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("기본 context가 문제없이 로드된다")
    void contextLoads() {
        // 이 테스트는 스프링 애플리케이션 컨텍스트가 정상적으로 로드되는지 확인합니다.
    }

    @Test
    @DisplayName("로그인 성공: 올바른 정보로 요청 시 200 OK 상태와 JWT 토큰을 반환한다")
    void login_success() throws Exception {
        // given (주어진 상황): 로그인 요청에 필요한 데이터를 준비합니다.
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("phone", "01012345678");
        loginRequest.put("password", "password1234");

        // when & then (실행 및 검증):
        // /api/auth/login 경로로 POST 요청을 보내고, 그 결과를 검증합니다.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))) // 요청 Body에 JSON 데이터를 담습니다.
                .andExpect(status().isOk()) // 1. HTTP 응답 코드가 200 (OK)인지 확인
                .andExpect(jsonPath("$.token").exists()) // 2. 응답 JSON 본문에 'token' 필드가 존재하는지 확인
                .andExpect(cookie().exists("ai_call_token")) // 3. 응답 헤더에 'ai_call_token' 쿠키가 설정되었는지 확인
                .andDo(print()); // 4. 요청과 응답의 전체 내용을 콘솔에 출력하여 디버깅에 활용
    }

    @Test
    @DisplayName("로그인 실패: 잘못된 비밀번호로 요청 시 401 Unauthorized 상태를 반환한다")
    void login_fail_with_wrong_password() throws Exception {
        // given: 잘못된 비밀번호를 포함한 로그인 요청 데이터를 준비합니다.
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("phone", "01012345678");
        loginRequest.put("password", "wrong-password-1234");

        // when & then:
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized()) // 1. HTTP 응답 코드가 401 (Unauthorized)인지 확인
                .andExpect(jsonPath("$.message").value("전화번호 또는 비밀번호가 올바르지 않습니다.")) // 2. 응답 JSON의 message 필드 값 확인
                .andDo(print());
    }
}
