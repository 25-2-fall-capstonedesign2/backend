package com.capstone.backend.config;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BinaryWebSocketConfig extends BinaryWebSocketHandler {

    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // WebSocket 연결이 성공하면 세션을 저장합니다.
        // 이때 세션의 고유 ID를 key로 사용합니다.
        sessions.put(session.getId(), session);
        System.out.println("Binary WebSocket connected: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 연결이 끊기면 맵에서 세션을 제거합니다.
        String callSessionId = getCallSessionId(session);
        if (callSessionId != null && !callSessionId.isEmpty()) {
            // 2. 추출한 '통화 세션 ID'를 Key로, 실제 연결된 WebSocketSession을 Value로 맵에 저장합니다.
            sessions.put(callSessionId, session);
            // 3. 어떤 WebSocketSession 객체에 어떤 ID가 매핑되었는지 추적하기 위해 session의 attribute에도 저장합니다.
            session.getAttributes().put("callSessionId", callSessionId);
            System.out.println("Binary WebSocket connected. Mapped callSessionId: " + callSessionId + " to session: " + session.getId());
        } else {
            // 통화 세션 ID가 없으면 비정상 연결로 간주하고 연결을 종료합니다.
            System.err.println("Connection attempt without a valid 'sessionId' query parameter. Closing session.");
            session.close(CloseStatus.BAD_DATA.withReason("sessionId query parameter is required."));
        }
    }

    /**
     * 특정 세션(클라이언트)에게 바이너리 데이터를 전송합니다.
     * @param sessionId 전송 대상 WebSocket 세션 ID
     * @param data 전송할 byte 배열 데이터
     */
    public void sendBinaryToClient(String sessionId, byte[] data) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new BinaryMessage(data));
            } catch (IOException e) {
                // 실제 프로덕션에서는 로깅 프레임워크(e.g., SLF4J) 사용을 권장합니다.
                System.err.println("Error sending binary message to " + sessionId + ": " + e.getMessage());
            }
        } else {
            System.err.println("Session not found or closed for ID: " + sessionId);
        }
    }

    /**
     * WebSocketSession의 URI에서 'sessionId' 쿼리 파라미터 값을 추출하는 헬퍼 메소드
     */
    private String getCallSessionId(WebSocketSession session) {
        // 테스트 HTML에서 sessionId를 쿼리 파라미터로 넘기지 않으므로 임시로 null을 허용합니다.
        // 실제 클라이언트 개발 시에는 Objects.requireNonNull을 사용하여 ID가 없는 경우를 차단해야 합니다.
        if (session.getUri() == null) return null;
        return UriComponentsBuilder.fromUri(session.getUri()).build()
                .getQueryParams()
                .getFirst("sessionId");
    }

    /**
     * 안드로이드 클라이언트로부터 받은 음성 데이터를 처리합니다.
     * 이 데이터는 GPU 서버로 전달되어야 합니다.
     * @param session 메시지를 보낸 클라이언트 세션
     * @param message 수신된 바이너리 메시지 (음성 데이터)
     */
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        // TODO: 여기서 WebSocketService를 호출하여 GPU 서버로 데이터를 전달하는 로직 구현 필요
        // 예: webSocketService.forwardAudioToGpu(session.getId(), message.getPayload().array());
        System.out.println("Received binary data of size: " + message.getPayloadLength() + " from " + session.getId());
    }
}