package com.capstone.backend.handler;

import com.capstone.backend.service.CallService;
import com.capstone.backend.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.nio.ByteBuffer; //  ByteBuffer 임포트
import java.nio.charset.StandardCharsets; // 텍스트 변환용

@Slf4j
@Component
@RequiredArgsConstructor
public class GpuWebSocketHandler extends BinaryWebSocketHandler {

    private final CallService callService;
    private final MessageService messageService;

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

    /*
      1 바이트를 읽고 이에 따라 다르게 행동하도록 분기된다
     */
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        ByteBuffer payload = message.getPayload();

        // 페이로드가 비어있거나 헤더조차 없으면 무시
        if (payload.remaining() < 1) {
            log.warn("Received empty or invalid binary message from GPU: {}", session.getId());
            return;
        }

        // 첫 1바이트를 읽어 메시지 타입을 확인
        byte messageType = payload.get(); // 첫 바이트(header)를 읽음

        // 실제 데이터 추출 (헤더를 제외한 나머지 바이트)
        byte[] dataBytes = new byte[payload.remaining()];
        payload.get(dataBytes);

        // CallService에서 현재 callSessionId 조회
        Long callSessionId = callService.getCallSessionIdByGpuSession(session.getId());
        if (callSessionId == null) {
            log.warn("Cannot find callSessionId for (unpaired?) GPU session: {}", session.getId());
            return; // 매칭이 안 된 세션이므로 무시
        }

        // AI팀과 약속한 바이너리 프로토콜 규격에 따라 분기
        switch (messageType) {
            case 0x01: // AI 오디오 청크
                // 헤더가 제거된 '순수 오디오 데이터'만 클라이언트로 전달
                callService.forwardToClient(session, new BinaryMessage(dataBytes));
                break;

            case 0x02: // 사용자 발화 텍스트 (GPU가 받아쓴 USER 텍스트)
                // UTF-8 텍스트로 디코딩
                String userText = new String(dataBytes, StandardCharsets.UTF_8);
                // DB에 "USER"로 저장
                messageService.saveMessage(callSessionId, "USER", userText);
                break;

            case 0x03: // AI 응답 텍스트 (GPU가 생성한 AI 텍스트)
                // UTF-8 텍스트로 디코딩
                String aiText = new String(dataBytes, StandardCharsets.UTF_8);
                // DB에 "AI"로 저장
                messageService.saveMessage(callSessionId, "AI", aiText);
                break;

            default:
                log.warn("Unknown binary message type received: {} from GPU: {}", messageType, session.getId());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        callService.disconnectGpu(session);
        log.info("GPU worker connection closed: Session ID = {}, Status = {}", session.getId(), status);
    }
}
