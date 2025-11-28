package com.capstone.backend.handler;

import com.capstone.backend.service.CallService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientWebSocketHandler extends BinaryWebSocketHandler {

    private final CallService callService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long callSessionId = getCallSessionId(session); // URI에서 ?sessionId=... 추출
        if (callSessionId == null) {
            log.warn("Invalid session ID. Closing connection.");
            session.close(CloseStatus.BAD_DATA.withReason("Missing or invalid sessionId"));
            return;
        }
        callService.registerClient(callSessionId, session);
        log.info("Client connection established: CallSessionId = {}", callSessionId);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        callService.forwardAudioToGpu(session, message);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        callService.clientDisconnected(session);
        log.info("Client connection closed: Session ID = {}, Status = {}", session.getId(), status);
    }

    // URI에서 sessionId 쿼리 파라미터 추출
    private Long getCallSessionId(WebSocketSession session) {
        try {
            URI uri = Objects.requireNonNull(session.getUri());
            String sessionIdStr = UriComponentsBuilder.fromUri(uri)
                    .build()
                    .getQueryParams()
                    .getFirst("sessionId");

            if (sessionIdStr != null) {
                return Long.parseLong(sessionIdStr);
            }
        } catch (Exception e) {
            log.warn("Failed to parse sessionId from URI: {}", e.getMessage());
        }
        return null;
    }
    /*
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        // [방어 로직] GPU가 준비되지 않았다면 오디오 패킷 무시
        if (!callService.isGpuReady(session)) {
            log.warn("Ignored audio packet: GPU is not ready yet.");
            return;
        }

        callService.forwardAudioToGpu(session, message);
    }*/
}
