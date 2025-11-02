package com.capstone.backend.handler;

import com.capstone.backend.service.CallService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoiceWebSocketHandler extends BinaryWebSocketHandler {

    private final CallService callService;

    // 세션을 관리하기 위한 맵
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    // 고객과 GPU 워커 세션을 매핑하기 위한 맵 (Key: customerSessionId, Value: gpuWorkerSessionId)
    private final Map<String, String> sessionPair = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 클라이언트가 연결되면 세션 맵에 추가
        callService.registerSession(session);
        log.info("New WebSocket connection established: Session ID = {}", session.getId());
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws IOException {
        // 바이너리 메시지(음성 데이터)를 받으면, 연결된 상대방 세션으로 그대로 전달합니다.
        log.info("Received binary message from {}", session.getId());
        callService.forwardMessage(session, message);

    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 연결이 끊기면 세션 맵에서 제거
        callService.removeSession(session);
        log.info("WebSocket connection closed: Session ID = {}, Status = {}", session.getId(), status);
    }
}