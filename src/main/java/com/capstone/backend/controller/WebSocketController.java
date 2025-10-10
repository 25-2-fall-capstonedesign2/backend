package com.capstone.backend.controller;

import com.capstone.backend.dto.VoiceMessage;
import com.capstone.backend.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final WebSocketService webSocketService;

    /**
     * 고객(Customer)으로부터 음성 데이터를 받는 엔드포인트
     */
    @MessageMapping("/audio-stream/{sessionId}")
    public void handleCustomerAudio(VoiceMessage message, @DestinationVariable String sessionId) {
        // 메시지에 세션 ID를 설정하여 서비스 로직으로 전달
        message.setSessionId(sessionId);
        webSocketService.forwardAudioToGpu(message);
    }

    /**
     * GPU Worker로부터 '오디오' 바이너리 결과를 받는 엔드포인트
     */
    @MessageMapping("/gpu-audio-result/{sessionId}")
    public void handleGpuAudioResult(@DestinationVariable String sessionId, @Payload byte[] audioData) {
        webSocketService.sendAudioToCustomer(sessionId, audioData);
    }

    /**
     * GPU Worker로부터 '텍스트' 결과를 받는 엔드포인트
     */
    @MessageMapping("/gpu-text-result/{sessionId}")
    public void handleGpuTextResult(@DestinationVariable String sessionId, @Payload String textData) {
        webSocketService.sendTextToCustomer(sessionId, textData);
    }
}
