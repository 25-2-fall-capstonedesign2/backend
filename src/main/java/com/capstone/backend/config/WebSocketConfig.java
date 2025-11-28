package com.capstone.backend.config;

import com.capstone.backend.handler.ClientWebSocketHandler;
import com.capstone.backend.handler.GpuWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

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

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        // 텍스트와 바이너리 메시지 버퍼 크기를 1MB로 설정 (기본값 8KB)
        container.setMaxTextMessageBufferSize(5 * 1024 * 1024);
        container.setMaxBinaryMessageBufferSize(5 * 1024 * 1024);
        return container;
    }
}