package com.capstone.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

@Configuration
@EnableWebSocketSecurity // 웹소켓 보안을 이 파일에서 전담하도록 설정
public class WebSocketSecurityConfig {

    @Bean
    public AuthorizationManager<Message<?>> messageAuthorizationManager(MessageMatcherDelegatingAuthorizationManager.Builder messages) {
        // 1. CONNECT 요청은 헤더 검증 없이 무조건 허용하여 연결 자체를 보장합니다.
        messages.simpTypeMatchers(SimpMessageType.CONNECT).permitAll()
                // 2. "/app/**"으로 가는 메시지는 인증된 사용자만 보내도록 설정할 수 있습니다. (지금은 테스트를 위해 permitAll)
                .simpDestMatchers("/app/**").permitAll()
                // 3. 그 외 다른 모든 메시지 유형(SUBSCRIBE 등)도 일단 허용합니다.
                .anyMessage().permitAll();

        return messages.build();
    }
}