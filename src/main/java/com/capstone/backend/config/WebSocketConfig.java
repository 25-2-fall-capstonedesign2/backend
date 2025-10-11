package com.capstone.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer, WebSocketConfigurer {

    private final BinaryWebSocketConfig binaryWebSocketConfig;

    /* stomp 방식 설정 - text, json 파일이 대상 */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns("*");
    }

    /**
     * 클라이언트로부터 들어오는 메시지(inbound) 채널을 구성합니다.
     * 이 메서드가 '보이지 않는 벽' 문제를 해결하는 열쇠입니다.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                // STOMP CONNECT 요청인 경우, 아무런 추가 검증 없이 통과시킵니다.
                // 이것으로 CONNECT(0) 문제를 최종적으로 해결합니다.
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // (필요 시 여기서 사용자 인증 정보를 헤더에 추가할 수 있습니다.)
                }
                return message;
            }
        });
    }

    /* 순수 WebSocket 관련 설정 -- audio data 가 대상 */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 음성(바이너리) 데이터용 WebSocket 엔드포인트
        registry.addHandler(binaryWebSocketConfig, "/ws-binary")
                .setAllowedOriginPatterns("*");
    }
}