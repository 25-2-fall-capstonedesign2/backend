package com.capstone.backend.service;

import com.capstone.backend.config.BinaryWebSocketConfig;
import com.capstone.backend.dto.TextMessage;
import com.capstone.backend.dto.VoiceMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final BinaryWebSocketConfig binaryWebSocketHandler;
    // private final MessageService messageService; // DB 저장을 위한 서비스 (별도 구현 필요)

    // GPU Worker들이 작업을 받기 위해 구독할 공용 토픽
    private static final String GPU_TASK_TOPIC = "/topic/gpu-tasks";

    /**
     * 고객의 음성 데이터를 GPU Worker에게 전달합니다.
     * @param voiceMessage 고객으로부터 받은 원본 메시지 (sessionId 포함)
     */
    public void forwardAudioToGpu(VoiceMessage voiceMessage) {
        log.info("Forwarding audio from session {} to GPU workers.", voiceMessage.getSessionId());

        // customerMessage 객체 그대로 GPU 토픽으로 전송
        // 이 메시지 안에는 어떤 고객의 요청인지 식별하기 위한 sessionId가 이미 들어있음
        messagingTemplate.convertAndSend(GPU_TASK_TOPIC, voiceMessage);
    }

    /**
     * [오디오] GPU가 보낸 바이너리 음성 데이터를 순수 WebSocket을 통해 고객에게 전달합니다.
     * (GPU -> 서버 -> 안드로이드)
     * @param customerSessionId 음성 데이터를 받을 클라이언트의 WebSocket 세션 ID
     * @param audioData 원본 byte[] 형태의 음성 데이터
     */
    public void sendAudioToCustomer(String customerSessionId, byte[] audioData) {
        if (customerSessionId == null || customerSessionId.isEmpty()) {
            log.error("GPU result is missing the original customer session ID.");
            return;
        }
        log.info("Sending binary audio data to session: {}", customerSessionId);
        String destination = "/topic/session/" + customerSessionId;

        // BinaryWebSocketConfig를 통해 직접 바이너리 메시지 전송
        binaryWebSocketHandler.sendBinaryToClient(customerSessionId, audioData);
    }

    /**
     * [텍스트] GPU가 보낸 문자열을 DB에 저장하고, STOMP를 통해 고객에게 전달합니다.
     * (GPU -> 서버 -> DB & 안드로이드)
     * @param customerSessionId 텍스트를 받을 클라이언트의 세션 ID
     * @param textData GPU가 생성한 대화 텍스트
     */
    public void sendTextToCustomer(String customerSessionId, String textData) {
        if (customerSessionId == null || customerSessionId.isEmpty()) {
            log.error("Cannot send text: customer session ID is missing.");
            return;
        }
        log.info("Sending text data to session {}: {}", customerSessionId, textData);

        // 1. DB에 AI가 보낸 메시지 저장 (MessageService를 통해 구현)
        // messageService.saveAiMessage(customerSessionId, textData);
        // log.info("AI message for session {} saved to DB.", customerSessionId);

        // 2. 클라이언트에게 전송할 목적지 주소
        String destination = "/topic/session/" + customerSessionId;

        // 3. TextMessage DTO를 사용하여 메시지 전송
        TextMessage payload = new TextMessage("transcript", "AI", textData, customerSessionId);
        messagingTemplate.convertAndSend(destination, payload);
    }
}