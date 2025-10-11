package com.capstone.backend.controller;

import com.capstone.backend.dto.TextMessage;
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
     * 안드로이드 클라이언트로부터 '음성 데이터'를 받는 엔드포인트입니다.
     * 클라이언트는 STOMP를 통해 이 주소로 VoiceMessage DTO를 전송합니다.
     * 받은 데이터는 GPU 워커에게 전달됩니다.
     */
    @MessageMapping("/audio.stream")
    public void handleClientAudio(@Payload VoiceMessage voiceMessage) {
        // VoiceMessage DTO에 이미 sessionId가 포함되어 있어야 합니다.
        webSocketService.forwardAudioToGpu(voiceMessage);
    }

    /**
     * 안드로이드 클라이언트로부터 '텍스트 데이터'를 받는 엔드포인트입니다. (향후 확장용)
     * 사용자가 직접 텍스트를 입력하는 경우를 대비합니다.
     */
    @MessageMapping("/text.message")
    public void handleClientText(@Payload TextMessage textMessage) {
        // webSocketService.processUserTextMessage(textMessage); // DB 저장 등 로직 호출
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
     * TODO: DB 저장 로직 필요
     */
    @MessageMapping("/gpu-text-result/{sessionId}")
    public void handleGpuTextResult(@DestinationVariable String sessionId, @Payload String textData) {
        webSocketService.sendTextToCustomer(sessionId, textData);
    }
}
