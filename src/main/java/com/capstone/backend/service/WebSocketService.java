package com.capstone.backend.service;

import com.capstone.backend.handler.AiModelWebSocketHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.client.support.WebSocketClientManager;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class WebSocketService {

    @Value("${ai.model.websocket.url}")
    private String aiModelUrl;

    @Getter // 핸들러에서 세션에 접근할 수 있도록 Getter 추가
    private WebSocketSession aiModelSession;

    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final WebSocketClientManager clientManager;

    // 생성자에서 WebSocketClientManager를 초기화합니다.
    public WebSocketService(@Value("${ai.model.websocket.url}") String aiModelUrl) {
        WebSocketClient client = new StandardWebSocketClient();
        // AiModelWebSocketHandler에 자기 자신(service)을 주입합니다.
        this.clientManager = new WebSocketClientManager(client, new AiModelWebSocketHandler(this), aiModelUrl);
        this.clientManager.setAutoStartup(true); // 자동 시작 설정
    }

    // 서버 시작 시 WebSocketClientManager를 실행합니다.
    @PostConstruct
    private void startConnectionManager() {
        log.info("Starting WebSocketClientManager to connect to AI Model at: {}", aiModelUrl);
        // Manager가 알아서 연결을 시도하고 유지합니다.
        this.clientManager.start();
    }

    // 서버 종료 시 Manager를 중지합니다.
    @PreDestroy
    private void stopConnectionManager() {
        log.info("Stopping WebSocketClientManager.");
        this.clientManager.stop();
    }

    // AiModelWebSocketHandler가 연결 성공 시 이 메서드를 호출하여 세션을 저장합니다.
    public void setAiModelSession(WebSocketSession session) {
        this.aiModelSession = session;
    }


    // 사용자 세션 등록
    public void registerUserSession(Long callSessionId, WebSocketSession session) {
        userSessions.put(callSessionId, session);
    }

    // 사용자 세션 제거
    public void removeUserSession(Long callSessionId) {
        userSessions.remove(callSessionId);
    }

    // 사용자 음성 데이터를 AI 모델로 전송
    public void sendToAiModel(Long callSessionId, BinaryMessage message) {
        if (aiModelSession != null && aiModelSession.isOpen()) {
            try {
                // TODO: 필요시 callSessionId를 데이터에 포함시켜 AI가 세션을 구분하게 할 수 있음
                // 예: 메시지 포맷 정의 { "sessionId": 123, "audio": "..." }
                aiModelSession.sendMessage(message);
            } catch (IOException e) {
                log.error("Error sending message to AI model for callSessionId: {}", callSessionId, e);
            }
        }
    }

    // AI 모델의 응답을 특정 사용자에게 전송
    public void sendToUser(Long callSessionId, BinaryMessage message) {
        WebSocketSession userSession = userSessions.get(callSessionId);
        if (userSession != null && userSession.isOpen()) {
            try {
                userSession.sendMessage(message);
            } catch (IOException e) {
                log.error("Error sending message to user for callSessionId: {}", callSessionId, e);
            }
        }
    }
}