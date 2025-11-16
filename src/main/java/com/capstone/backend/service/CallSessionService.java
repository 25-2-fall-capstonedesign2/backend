package com.capstone.backend.service;

import com.capstone.backend.entity.CallSession;
import com.capstone.backend.entity.User;
import com.capstone.backend.repository.CallSessionRepository;
import com.capstone.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException; // 예외 추가

// DB의 call_session 테이블만 관리해서 고객에게 자동 부여
@Slf4j
@Service
@RequiredArgsConstructor
public class CallSessionService {

    private final CallSessionRepository callSessionRepository;
    private final UserRepository userRepository;

    /**
     * 새로운 통화 세션을 생성하고 DB에 저장합니다.
     *
     * @param userPhoneNumber (JWT 토큰에서 식별된 사용자의 전화번호)
     * @param participantName (새로 추가됨: 통화 대상의 이름)
     * @return 생성된 call_session_id
     */
    @Transactional
    public Long createCallSession(String userPhoneNumber, String participantName) {
        // 1. 전화번호를 기반으로 User 엔티티를 조회합니다.
        User user = userRepository.findByPhoneNumber(userPhoneNumber)
                .orElseThrow(() -> new UsernameNotFoundException("인증된 사용자를 찾을 수 없습니다: " + userPhoneNumber));

        // 2. 새 CallSession 객체를 생성합니다.
        //    (startTime은 @CreationTimestamp에 의해 자동 생성됩니다)
        CallSession newSession = CallSession.builder()
                .user(user)
                .participantName(participantName)
                .build();

        // 3. DB에 저장합니다.
        CallSession savedSession = callSessionRepository.save(newSession);

        // 4. 생성된 ID를 반환합니다.
        return savedSession.getId();
    }

    /**
     * 통화 세션을 종료하고 DB에 end_time을 기록합니다.
     *
     * @param callSessionId 종료할 통화 ID
     * @param userPhoneNumber 인증된 사용자(JWT)의 전화번호
     */
    @Transactional
    public void endCallSession(Long callSessionId, String userPhoneNumber) {
        // 1. DB에서 세션 조회
        CallSession callSession = callSessionRepository.findById(callSessionId)
                .orElseThrow(() -> new RuntimeException("통화 세션을 찾을 수 없습니다: " + callSessionId));

        // 2. (보안) 이 통화가 요청자의 통화가 맞는지 확인
        String ownerPhoneNumber = callSession.getUser().getPhoneNumber();
        if (!ownerPhoneNumber.equals(userPhoneNumber)) {
            log.warn("User {} tried to end session {} owned by {}", userPhoneNumber, callSessionId, ownerPhoneNumber);
            throw new AccessDeniedException("이 통화 세션을 종료할 권한이 없습니다.");
        }

        // 3. (중복 방지) 이미 종료되었는지 확인
        if (callSession.getEndTime() != null) {
            log.warn("Session {} is already ended.", callSessionId);
            throw new EntityNotFoundException("이미 종료되었거나 존재하지 않는 세션입니다."); //404 error 반환
        }

        // 4. 종료 시간 기록
        callSession.endCall(); // (지난번에 엔티티에 만든 편의 메서드 사용)
        callSessionRepository.save(callSession);

        log.info("Call session {} ended for user {}", callSessionId, userPhoneNumber);
    }
}
