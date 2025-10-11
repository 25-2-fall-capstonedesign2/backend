package com.capstone.backend.config;

import com.capstone.backend.dto.VoiceMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableWebSocketMessageBroker // STOMP 활성화는 DelegatingWebSocketMessageBrokerConfiguration를 유발하므로 삭제
@RequiredArgsConstructor
public class WebSocketConfig extends BinaryWebSocketHandler implements WebSocketConfigurer {

    private final SimpMessagingTemplate messagingTemplate;

    private static final String GPU_TASK_TOPIC = "/topic/gpu-tasks";
    private static final Map<String, WebSocketSession> clientSessions = new ConcurrentHashMap<>();

    // --- 순수 WebSocket 핸들러 로직 (기존 BinaryWebSocketConfig의 내용) ---
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String callSessionId = getQueryParam(session, "sessionId");
        String userType = getQueryParam(session, "type");

        if (callSessionId == null || userType == null) {
            session.close(CloseStatus.BAD_DATA.withReason("sessionId and type parameters are required."));
            return;
        }

        if ("client".equals(userType)) {
            clientSessions.put(callSessionId, session);
            session.getAttributes().put("callSessionId", callSessionId);
            System.out.println("Client connected via binary WebSocket. Mapped callSessionId: " + callSessionId);
        } else {
            System.out.println("GPU worker connected via binary WebSocket for callSessionId: " + callSessionId);
        }
        session.getAttributes().put("userType", userType);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        String userType = (String) session.getAttributes().get("userType");
        String callSessionId = getQueryParam(session, "sessionId");
        byte[] audioData = message.getPayload().array();

        if ("client".equals(userType)) {
            // 고객 오디오 -> GPU 작업 토픽으로 발행
            System.out.println("Received " + audioData.length + " bytes from CLIENT for " + callSessionId);
            VoiceMessage voiceMessage = new VoiceMessage("audio_chunk", audioData, callSessionId);
            messagingTemplate.convertAndSend(GPU_TASK_TOPIC, voiceMessage);
        } else if ("gpu".equals(userType)) {
            // GPU 오디오 -> 해당 고객에게 직접 전달
            System.out.println("Received " + audioData.length + " bytes from GPU for " + callSessionId);
            sendBinaryToClient(callSessionId, audioData);
        }
    }

    public void sendBinaryToClient(String callSessionId, byte[] data) {
        WebSocketSession session = clientSessions.get(callSessionId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new BinaryMessage(data));
            } catch (IOException e) {
                System.err.println("Error sending binary message to " + callSessionId + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        if ("client".equals(session.getAttributes().get("userType"))) {
            String callSessionId = (String) session.getAttributes().get("callSessionId");
            if (callSessionId != null) {
                clientSessions.remove(callSessionId);
                System.out.println("Client binary WebSocket disconnected. Removed mapping for: " + callSessionId);
            }
        } else {
            System.out.println("GPU worker binary WebSocket disconnected.");
        }
    }

    private String getQueryParam(WebSocketSession session, String key) {
        if (session.getUri() == null) return null;
        return UriComponentsBuilder.fromUri(session.getUri()).build().getQueryParams().getFirst(key);
    }

    // --- WebSocketConfigurer 구현 (핸들러 등록) ---
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // "/ws-binary" 경로의 요청을 이 클래스(자기 자신)가 처리하도록 등록
        registry.addHandler(this, "/ws-binary").setAllowedOriginPatterns("*");
    }
}