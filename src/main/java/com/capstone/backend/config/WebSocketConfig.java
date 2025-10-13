package com.capstone.backend.config;

import com.capstone.backend.handler.VoiceWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket // 순수 WebSocket 활성화
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final VoiceWebSocketHandler voiceWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // ws://서버주소/ws/voice 로 들어오는 요청을 voiceWebSocketHandler가 처리
        registry.addHandler(voiceWebSocketHandler, "/ws/voice")
                .setAllowedOriginPatterns("*"); // 모든 출처 허용
    }
}