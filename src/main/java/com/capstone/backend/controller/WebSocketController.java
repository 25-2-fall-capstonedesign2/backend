package com.capstone.backend.controller;

import com.capstone.backend.dto.VoiceMessage;
import com.capstone.backend.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    // (나중에 추가될 WebSocketService를 위한 자리입니다)
    private final WebSocketService webSocketService;

    /**
     * 클라이언트로부터 오는 모든 WebSocket 메시지를 처리하는 단일 엔드포인트.
     * 클라이언트는 STOMP Destination을 "/app/stream"으로 설정해야 합니다.
     * @param message 수신된 메시지 (type, data, sessionId 포함)
     */
    @MessageMapping("/stream")
    public void handleStreamMessage(@Payload VoiceMessage message) {
        log.info("Received WebSocket message: {}", message);

        if (message.getType() == null || message.getSessionId() == null) {
            log.warn("Received message with null type or sessionId: {}", message);
            return;
        }

        // 메시지 타입에 따라 서비스 로직 분기
        switch (message.getType()) {
            case "audio_chunk":
                webSocketService.processAudioChunk(message);
                break;

            case "text_chunk": // 'command'에서 'text_chunk'로 변경
                webSocketService.processTextChunk(message); // 호출할 메서드 이름 변경
                break;

            default:
                log.warn("Received unknown message type '{}' for session: {}", message.getType(), message.getSessionId());
                break;
        }
    }
}
