package com.capstone.backend.service;

import com.capstone.backend.dto.VoiceMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    // GPU Worker들이 작업을 받기 위해 구독할 공용 토픽
    private static final String GPU_TASK_TOPIC = "/topic/gpu-tasks";

    /**
     * 고객의 음성 데이터를 GPU Worker에게 전달합니다.
     * @param customerMessage 고객으로부터 받은 원본 메시지 (sessionId 포함)
     */
    public void forwardAudioToGpu(VoiceMessage customerMessage) {
        log.info("Forwarding audio from session {} to GPU workers.", customerMessage.getSessionId());

        // customerMessage 객체 그대로 GPU 토픽으로 전송
        // 이 메시지 안에는 어떤 고객의 요청인지 식별하기 위한 sessionId가 이미 들어있음
        messagingTemplate.convertAndSend(GPU_TASK_TOPIC, customerMessage);
    }

    /**
     * [오디오 처리] GPU가 보낸 바이너리 음성 데이터를 고객에게 그대로 전달합니다.
     */
    public void sendAudioToCustomer(String customerSessionId, byte[] audioData) {
        if (customerSessionId == null || customerSessionId.isEmpty()) {
            log.error("GPU result is missing the original customer session ID.");
            return;
        }
        log.info("Sending binary audio data to session: {}", customerSessionId);
        String destination = "/topic/session/" + customerSessionId;

        // 헤더에 바이너리 타입을 명시하여 전송
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
        headerAccessor.setDestination(destination);
        headerAccessor.setContentType(MimeTypeUtils.APPLICATION_OCTET_STREAM);

        messagingTemplate.convertAndSend(destination, audioData, headerAccessor.getMessageHeaders());
    }

    /**
     * [텍스트 처리] GPU가 보낸 문자열을 JSON 객체로 변환하여 고객에게 전달합니다.
     */
    public void sendTextToCustomer(String customerSessionId, String textData) {
        log.info("Sending text data to session {}: {}", customerSessionId, textData);
        String destination = "/topic/session/" + customerSessionId;

        // ❗️ DTO 대신 Map을 사용하여 보낼 JSON 페이로드를 직접 구성합니다.
        // 이렇게 하면 data 필드를 String 타입으로 자유롭게 보낼 수 있습니다.
        // TODO: DB 저장 로직 구현 필요
        Map<String, String> payload = new HashMap<>();
        payload.put("type", "transcript");
        payload.put("sessionId", customerSessionId);
        payload.put("data", textData); // textData는 String 타입


        messagingTemplate.convertAndSend(destination, payload);
    }
}