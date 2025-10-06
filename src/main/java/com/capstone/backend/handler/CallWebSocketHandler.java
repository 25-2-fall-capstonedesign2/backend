package com.capstone.backend.handler;

import com.capstone.backend.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class CallWebSocketHandler extends BinaryWebSocketHandler {

    private final WebSocketService webSocketService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long callSessionId = extractCallSessionId(session);
        if (callSessionId != null) {
            log.info("User connection established for call: {}", callSessionId);
            webSocketService.registerUserSession(callSessionId, session);
        } else {
            log.warn("Connection attempt with invalid callSessionId in URI: {}", session.getUri());
            session.close(CloseStatus.BAD_DATA.withReason("Invalid callSessionId"));
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        Long callSessionId = extractCallSessionId(session);
        // 받은 음성 데이터를 WebSocketService를 통해 AI 모델로 전달
        webSocketService.sendToAiModel(callSessionId, message);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long callSessionId = extractCallSessionId(session);
        if (callSessionId != null) {
            log.info("User connection closed for call: {} with status: {}", callSessionId, status);
            webSocketService.removeUserSession(callSessionId);
        }
    }

    private Long extractCallSessionId(WebSocketSession session) {
        String path = session.getUri().getPath();
        try {
            return Long.parseLong(path.substring(path.lastIndexOf('/') + 1));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}