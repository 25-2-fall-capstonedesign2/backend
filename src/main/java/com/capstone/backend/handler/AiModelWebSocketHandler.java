package com.capstone.backend.handler;

import com.capstone.backend.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

@Slf4j
@RequiredArgsConstructor
public class AiModelWebSocketHandler extends BinaryWebSocketHandler {

    private final WebSocketService webSocketService;

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        log.debug("Received message from AI Model");
        // TODO: AI 모델 응답에서 callSessionId를 파싱해야 함
        // 현재는 Echo 서버 테스트를 위해 임의의 ID (예: 첫 번째 사용자)로 전달
        // 실제로는 AI 모델이 응답에 세션 ID를 포함해줘야 함.
        Long targetCallSessionId = 1L; // <<-- 이 부분은 AI 모델과의 협의가 필요합니다.
        webSocketService.sendToUser(targetCallSessionId, message);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("AI Model WebSocket transport error", exception);
        // TODO: 에러 발생 시 재연결 로직 호출
    }
}
