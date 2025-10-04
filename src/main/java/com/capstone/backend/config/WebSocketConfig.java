package com.capstone.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // STOMP를 사용하기 위해 선언하는 어노테이션
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라이언트에서 서버로 메시지를 보낼 때 사용하는 prefix
        registry.setApplicationDestinationPrefixes("/app");
        // 구독자(클라이언트)들에게 메시지를 보낼 때 사용하는 prefix
        registry.enableSimpleBroker("/topic");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 클라이언트들이 WebSocket 연결을 시작할 주소(endpoint)
        // 로컬 GPU 컴퓨터가 이 주소로 연결을 시도할 겁니다.
        registry.addEndpoint("/ws-stomp") // 연결 주소: /ws-stomp
                .setAllowedOriginPatterns("*") // 모든 출처에서의 연결을 허용 (CORS 설정)
                .withSockJS(); // SockJS 지원을 활성화 (WebSocket을 지원하지 않는 환경을 위한 대비책)
    }
}