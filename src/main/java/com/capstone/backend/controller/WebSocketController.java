package com.capstone.backend.controller;

import com.capstone.backend.dto.VoiceMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    /**
     * 로컬 GPU 클라이언트로부터 처리 결과를 받는 엔드포인트
     * 클라이언트가 "/app/gpu-result"로 메시지를 보내면 이 메서드가 처리합니다.
     */
    @MessageMapping("/gpu-result")
    @SendTo("/topic/results") // 처리 결과를 "/topic/results"를 구독하는 모든 클라이언트에게 보냅니다.
    public VoiceMessage handleGpuResult(VoiceMessage message) {
        // GPU로부터 받은 처리 결과(message)를 가지고 비즈니스 로직 수행
        System.out.println("GPU로부터 결과 수신: " + message.getData());

        // 여기서 받은 결과를 안드로이드 앱에 다시 보내주는 등의 로직이 필요할 수 있습니다.
        // 지금은 예시로 받은 메시지를 그대로 다시 브로드캐스팅합니다.
        return message;
    }
}
