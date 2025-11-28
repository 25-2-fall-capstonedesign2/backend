package com.capstone.backend;

import com.capstone.backend.dto.SignupRequestDto;
import com.capstone.backend.entity.User;
import com.capstone.backend.repository.UserRepository;
import com.capstone.backend.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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

    // JWT 생성을 위해 추가
    @Autowired
    private JwtUtil jwtUtil;


    /**
     * 각 테스트가 실행되기 전에 항상 먼저 실행되는 메소드.
     * 테스트에 필요한 데이터를 미리 H2 DB에 준비합니다.
     */
    @BeforeEach
    void setUp() {
        userRepository.deleteAll(); // 이전 테스트 데이터를 깔끔하게 지웁니다.
        User testUser = new User("01012345678",
                    passwordEncoder.encode("password1234"),
                "테스트유저");
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

    // --- 회원가입 테스트 ---
    @Test
    @DisplayName("회원가입 성공")
    void signup_success() throws Exception {
        // given
        SignupRequestDto signupRequestDto = new SignupRequestDto();
        signupRequestDto.setPhone("01099998888");
        signupRequestDto.setPassword("newpassword123");
        signupRequestDto.setDisplayName("신규유저");

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."))
                .andDo(print());

        // DB에 실제로 저장되었는지, 비밀번호는 암호화되었는지 검증
        User signedUpUser = userRepository.findByPhoneNumber("01099998888").orElseThrow();
        assertEquals("신규유저", signedUpUser.getDisplayName());
        assertTrue(passwordEncoder.matches("newpassword123", signedUpUser.getPassword()));
    }

    @Test
    @DisplayName("회원가입 실패 - 중복된 전화번호")
    void signup_fail_duplicate_phone() throws Exception {
        // given
        SignupRequestDto signupRequestDto = new SignupRequestDto();
        signupRequestDto.setPhone("01012345678"); // 이미 존재하는 전화번호
        signupRequestDto.setPassword("anotherpassword");
        signupRequestDto.setDisplayName("중복된유저");

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequestDto)))
                .andExpect(status().isBadRequest()) // 400 Bad Request
                .andExpect(jsonPath("$.message").value("이미 사용 중인 전화번호입니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("회원가입 실패 - 유효성 검사 (짧은 비밀번호)")
    void signup_fail_validation() throws Exception {
        // given
        SignupRequestDto signupRequestDto = new SignupRequestDto();
        signupRequestDto.setPhone("01011112222");
        signupRequestDto.setPassword("1234"); // 8자 미만 비밀번호
        signupRequestDto.setDisplayName("유효성실패유저");

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequestDto)))
                .andExpect(status().isBadRequest()) // @Valid에 의해 400 에러 발생
                .andDo(print());
    }

    // --- 회원탈퇴 테스트 ---
    @Test
    @DisplayName("회원탈퇴 성공")
    void withdraw_success() throws Exception {
        // given
        String phoneNumber = "01012345678";
        String token = jwtUtil.generateToken(phoneNumber); // 테스트용 토큰 발급

        // when & then
        mockMvc.perform(delete("/api/auth/withdraw")
                        .header("Authorization", "Bearer " + token)) // 인증 헤더 추가
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("회원탈퇴가 성공적으로 처리되었습니다."))
                .andDo(print());

        // DB에서 실제로 삭제되었는지 검증
        Optional<User> withdrawnUser = userRepository.findByPhoneNumber(phoneNumber);
        assertFalse(withdrawnUser.isPresent());
    }

    @Test
    @DisplayName("회원탈퇴 실패 - 인증 토큰 없음")
    void withdraw_fail_unauthorized() throws Exception {
        // given (인증 토큰 없이 요청)

        // when & then
        mockMvc.perform(delete("/api/auth/withdraw"))
                .andExpect(status().isUnauthorized()) // Spring Security는 인증 실패 시 기본적으로 403 Forbidden 반환
                .andDo(print());
    }
}
