package com.capstone.backend.controller;

import com.capstone.backend.dto.VoiceMessage;
import com.capstone.backend.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    // (나중에 추가될 WebSocketService를 위한 자리입니다)
    private final WebSocketService webSocketService;

    /**
     * 클라이언트가 "/app/send-audio" 경로로 메시지를 보내면 이 메서드가 호출됩니다.
     * @param message 클라이언트가 보낸 VoiceMessage DTO
     */
    @MessageMapping("/send-audio")
    public void handleAudioFromClient(VoiceMessage message) {
        log.info("클라이언트로부터 메시지 수신 (Session ID: {})", message.getSessionId());

        // TODO: 여기서 webSocketService를 호출하여 AI 서버로 메시지를 전달하는 로직이 추가될 예정입니다.
        webSocketService.sendToAiModel(message);
    }
}
