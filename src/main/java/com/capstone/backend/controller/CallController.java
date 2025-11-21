package com.capstone.backend.controller;

import com.capstone.backend.dto.CreateCallRequestDto;
import com.capstone.backend.dto.CreateCallResponseDto;
import com.capstone.backend.dto.VoiceMessageDto;
import com.capstone.backend.service.CallService;
import com.capstone.backend.service.CallSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/calls")
@RequiredArgsConstructor
public class CallController {

    private final CallSessionService callSessionService;
    private final CallService callService;
    /**
     * [POST /api/calls/start]
     * Flutter 앱에서 '통화 시작' 시 호출하는 API입니다.
     * 인증된 사용자의 새 통화 세션을 생성하고 세션 ID를 반환합니다.
     *
     * @param principal Spring Security가 JWT 토큰을 해석하여 넣어주는 사용자 정보
     * @param requestDto Flutter가 보낸 JSON ({"participantName": "선택한이름"})
     * @return {"callSessionId": 123} 형태의 JSON 응답
     */

    @PostMapping("/start")
    public ResponseEntity<CreateCallResponseDto> startCall(
            Principal principal,
            @RequestBody CreateCallRequestDto requestDto) {

        String userPhoneNumber = principal.getName();
        Long voiceProfileId = requestDto.getVoiceProfileId();

        // 1. DB에 세션만 생성 (GPU 매칭 X)
        Long sessionId = callSessionService.createCallSession(userPhoneNumber, requestDto.getVoiceProfileId());

        VoiceMessageDto startMessage = VoiceMessageDto.builder()
                .type("start")
                .sessionId(String.valueOf(sessionId))
                .voiceProfileId(voiceProfileId)
                .build();

        // 2. 생성된 ID 반환 -> 앱은 이걸 받고 WebSocket 연결 시도함
        return ResponseEntity.ok(new CreateCallResponseDto(sessionId));
    }

    /**
     * [POST /api/calls/{callSessionId}/hangup]
     * Flutter 앱에서 '통화 종료' 시 호출하는 API입니다.
     * DB의 end_time을 기록하고, 실시간 WebSocket 연결을 종료시킵니다.
     *
     * @param callSessionId 종료할 통화 ID
     * @param principal 인증된 사용자 정보
     * @return 200 OK
     */
    @PostMapping("/{callSessionId}/hangup")
    public ResponseEntity<Void> endCallSession(
            @PathVariable Long callSessionId,
            Principal principal) {

        String userPhoneNumber = principal.getName();

        // 1. (DB 작업) DB에 end_time 기록 및 권한 확인
        callSessionService.endCallSession(callSessionId);

        // 2. (실시간 작업) WebSocket 연결 강제 종료 및 정리
        callService.forceDisconnect(callSessionId);

        return ResponseEntity.ok().build();
    }
}
