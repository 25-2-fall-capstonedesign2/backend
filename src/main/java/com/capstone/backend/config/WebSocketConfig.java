package com.capstone.backend.config;

import com.capstone.backend.handler.CallWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor // final 필드에 대한 생성자 자동 생성
public class WebSocketConfig implements WebSocketConfigurer {

    private final CallWebSocketHandler callWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(callWebSocketHandler, "/ws/call/{callSessionId}")
                .setAllowedOrigins("*");
    }
}