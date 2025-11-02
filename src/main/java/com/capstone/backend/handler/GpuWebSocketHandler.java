package com.capstone.backend.handler;

import com.capstone.backend.service.CallService;
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
public class GpuWebSocketHandler extends BinaryWebSocketHandler {

    private final CallService callService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception{
        try {
            // TODO: ?workerId=... 파라미터로 GPU 워커 식별
            callService.registerGpu(session);
            log.info("GPU worker connection established: Session ID = {}", session.getId());
        } catch(Exception e) {
            log.error("Failed to establish GPU worker connection: {}", e.getMessage(), e);
            session.close();
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        callService.forwardToClient(session, message);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        callService.disconnectGpu(session);
        log.info("GPU worker connection closed: Session ID = {}, Status = {}", session.getId(), status);
    }
}
