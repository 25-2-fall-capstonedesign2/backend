package com.capstone.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // STOMP를 사용하기 위해 이 어노테이션을 선언합니다.
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 클라이언트가 WebSocket 연결을 맺기 위한 최초의 접속 지점(Endpoint)을 설정합니다.
        registry.addEndpoint("/ws-stomp") // 엔드포인트: /ws-stomp
                .setAllowedOriginPatterns("*"); // 모든 출처(CORS)에서의 연결을 허용합니다.
        //.withSockJS(); // WebSocket을 지원하지 않는 브라우저를 위한 SockJS 옵션을 활성화합니다.
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 1. 클라이언트 -> 서버 메시지 발행(publish) 경로 Prefix
        // 클라이언트가 "/app"으로 시작하는 경로로 메시지를 보내면, 서버의 @MessageMapping 메서드로 라우팅됩니다.
        registry.setApplicationDestinationPrefixes("/app");

        // 2. 서버 -> 클라이언트 메시지 구독(subscribe) 경로 Prefix
        // "/topic"으로 시작하는 주제를 구독하는 클라이언트에게 메시지 브로커가 메시지를 전파합니다.
        registry.enableSimpleBroker("/topic");
    }
}