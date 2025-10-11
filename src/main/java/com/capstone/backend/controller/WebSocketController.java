package com.capstone.backend.controller;

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
     * GPU Worker로부터 '텍스트' 결과를 받는 엔드포인트.
     * 받은 텍스트는 서비스 레이어를 통해 DB에 저장됩니다.
     */
    @MessageMapping("/gpu-text-result/{sessionId}")
    public void handleGpuTextResult(@DestinationVariable String sessionId, @Payload String textData) {
        webSocketService.saveTextToDatabase(sessionId, textData);
    }
}