package com.capstone.backend.service;

import com.capstone.backend.dto.MessageDto;
import com.capstone.backend.entity.CallSession;
import com.capstone.backend.entity.User;
import com.capstone.backend.repository.CallSessionRepository;
import com.capstone.backend.repository.MessageRepository;
import com.capstone.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 조회만 하도록
public class CallHistoryService {
    private final UserRepository userRepository;
    private final CallSessionRepository callSessionRepository;
    private final MessageRepository messageRepository;

    /**
     * 현재 사용자의 모든 통화 대상 목록 (중복 제거)
     */
    public List<String> getParticipantList(String userPhoneNumber) {
        User user = findUserByPhoneNumber(userPhoneNumber);

        // (3단계에서 CallSessionRepository에 추가할 메서드)
        return callSessionRepository.findDistinctParticipantNameByUser(user);
    }

    /**
     * 특정 통화 대상과의 모든 메시지 내역 (시간순)
     */
    public List<MessageDto> getMessagesByParticipant(String userPhoneNumber, String participantName) {
        User user = findUserByPhoneNumber(userPhoneNumber);

        // 1. 이 사용자와 이 참가자 간의 '모든' 통화 세션(CallSession)을 찾습니다.
        // (3단계에서 CallSessionRepository에 추가할 메서드)
        List<CallSession> sessions = callSessionRepository
                .findAllByUserAndParticipantName(user, participantName);

        // 2. 이 세션들에 속한 '모든' 메시지를 시간순으로 찾습니다.
        // (4단계에서 MessageRepository에 추가할 메서드)
        return messageRepository.findAllByCallSessionInOrderByTimestampAsc(sessions)
                .stream()
                .map(MessageDto::new) // Message 엔티티 -> MessageDto로 변환
                .collect(Collectors.toList());
    }

    // 공통 헬퍼 메서드
    private User findUserByPhoneNumber(String userPhoneNumber) {
        return userRepository.findByPhoneNumber(userPhoneNumber)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userPhoneNumber));
    }
}
