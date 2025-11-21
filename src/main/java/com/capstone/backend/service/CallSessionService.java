package com.capstone.backend.service;

import com.capstone.backend.entity.CallSession;
import com.capstone.backend.entity.User;
import com.capstone.backend.entity.VoiceProfile;
import com.capstone.backend.repository.CallSessionRepository;
import com.capstone.backend.repository.UserRepository;
import com.capstone.backend.repository.VoiceProfileRepository;
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
    private final VoiceProfileRepository voiceProfileRepository;

    /**
     * 새로운 통화 세션을 생성하고 DB에 저장합니다.
     *
     * @param userPhoneNumber (JWT 토큰에서 식별된 사용자의 전화번호)
     * @param voiceProfileId (통화 대상의 id)
     * @return 생성된 call_session_id
     */
    @Transactional
    public Long createCallSession(String userPhoneNumber, Long voiceProfileId) {
        // 1. 사용자 조회
        User user = userRepository.findByPhoneNumber(userPhoneNumber)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userPhoneNumber));

        // 2. [수정됨] ID로 목소리 프로필 조회
        VoiceProfile voiceProfile = voiceProfileRepository.findById(voiceProfileId)
                .orElseThrow(() -> new EntityNotFoundException("Voice profile not found: " + voiceProfileId));

        // 3. [추가됨] 보안 검증: 내 목소리 프로필이 맞는지 확인 (남의 것 사용 방지)
        if (!voiceProfile.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("이 목소리 프로필에 대한 접근 권한이 없습니다.");
        }

        // 4. 세션 생성 및 저장
        CallSession newSession = CallSession.builder()
                .user(user)
                .voiceProfile(voiceProfile)
                .build();

        CallSession savedSession = callSessionRepository.save(newSession);

        return savedSession.getId();
    }

    /**
     * 통화 세션을 종료하고 DB에 end_time을 기록합니다.
     *
     * @param callSessionId 종료할 통화 ID
     */
    @Transactional
    public void endCallSession(Long callSessionId) {
        CallSession session = callSessionRepository.findById(callSessionId)
                .orElseThrow(() -> new EntityNotFoundException("Session not found"));

        if (session.getEndTime() == null) {
            session.endCall();
        }
    }

    // [중요] CallService에서 GPU에게 VoiceID를 알려주기 위해 필요함
    @Transactional(readOnly = true)
    public Long getVoiceProfileId(Long callSessionId) {
        return callSessionRepository.findById(callSessionId)
                .map(session -> session.getVoiceProfile().getId())
                .orElseThrow(() -> new EntityNotFoundException("Session not found"));
    }
}
