package com.capstone.backend.controller;

import com.capstone.backend.dto.MessageDto;
import com.capstone.backend.service.CallHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/history")
@RequiredArgsConstructor
public class CallHistoryController {
    private final CallHistoryService callHistoryService;

    /**
     * [GET /api/v1/history/participants]
     * 현재 로그인한 사용자의 통화 대상 목록 (중복 제거)
     */
    @GetMapping("/participants") // 2. 참가자 목록 API 엔드포인트
    public ResponseEntity<List<String>> getParticipantList(Principal principal) {
        String userPhoneNumber = principal.getName();
        List<String> participants = callHistoryService.getParticipantList(userPhoneNumber);
        return ResponseEntity.ok(participants);
    }

    /**
     * [GET /api/v1/history/messages?profileName=...]
     * 특정 통화 대상과의 모든 메시지 내역 (시간순)
     */
    @GetMapping("/messages") // 3. 메시지 내역 API 엔드포인트
    public ResponseEntity<List<MessageDto>> getMessageHistory(
            Principal principal,
            @RequestParam("profileName") String participantName
    ) {
        String userPhoneNumber = principal.getName();
        List<MessageDto> messages = callHistoryService.getMessagesByParticipant(userPhoneNumber, participantName);
        return ResponseEntity.ok(messages);
    }
}
