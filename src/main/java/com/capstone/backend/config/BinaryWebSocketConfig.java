package com.capstone.backend.config;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BinaryWebSocketConfig extends BinaryWebSocketHandler {

    // 세션 ID와 WebSocketSession을 매핑하여 저장
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 연결 시 클라이언트가 보낸 고유 ID(예: 쿼리 파라미터)를 세션 ID로 사용
        // 예: ws://localhost:8080/ws-binary?sessionId=고객세션ID
        String sessionId = (String) session.getAttributes().get("sessionId");
        if (sessionId != null && !sessionId.isEmpty()) {
            sessions.put(sessionId, session);
            System.out.println("Binary WebSocket connected: " + sessionId);
        } else {
            System.out.println("Connection without session ID rejected.");
            session.close();
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        // 이 핸들러는 클라이언트 -> 서버로 바이너리 보낼 때 사용 (현재 요구사항에는 없음)
        // 필요하다면 여기에 로직 구현
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = (String) session.getAttributes().get("sessionId");
        if (sessionId != null) {
            sessions.remove(sessionId);
            System.out.println("Binary WebSocket disconnected: " + sessionId);
        }
    }

    // WebSocketService에서 이 메소드를 호출하여 특정 클라이언트에게 바이너리 데이터 전송
    public void sendBinaryToClient(String sessionId, byte[] data) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new BinaryMessage(data));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
