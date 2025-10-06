package com.capstone.backend.controller;

import com.capstone.backend.dto.VoiceMessage;
import com.capstone.backend.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor // final 필드에 대한 생성자 자동 주입
public class WebSocketController {

    private final WebSocketService webSocketService;

    /**
     * 안드로이드 클라이언트로부터 음성 데이터를 받아 GPU 서버로 전송을 요청하는 엔드포인트
     * 클라이언트가 "/app/send-audio"로 메시지를 보내면 이 메서드가 처리합니다.
     * @SendTo 어노테이션을 제거하여, 메시지 브로드캐스팅을 방지합니다.
     */
    @MessageMapping("/send-audio")
    public void handleAudioFromClient(VoiceMessage message) {
        log.info("클라이언트로부터 음성 데이터 수신 (Session ID: {})", message.getSessionId());
        // 받은 메시지를 그대로 서비스 레이어로 전달하여 로직 처리를 위임합니다.
        webSocketService.sendToGpuServer(message);
    }
}
