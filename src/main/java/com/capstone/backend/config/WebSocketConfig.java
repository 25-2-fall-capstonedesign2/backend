package com.capstone.backend.config;

import com.capstone.backend.handler.ClientWebSocketHandler;
import com.capstone.backend.handler.GpuWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket // 순수 WebSocket 활성화
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ClientWebSocketHandler clientWebSocketHandler;
    private final GpuWebSocketHandler gpuWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 1. 고객 전용 엔드포인트
        registry.addHandler(clientWebSocketHandler, "/ws-client")
                .setAllowedOriginPatterns("*");

        // 2. GPU 워커 전용 엔드포인트
        registry.addHandler(gpuWebSocketHandler, "/ws-gpu")
                .setAllowedOriginPatterns("*");
    }
}