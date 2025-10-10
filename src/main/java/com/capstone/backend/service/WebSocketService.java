package com.capstone.backend.service;

import com.capstone.backend.dto.VoiceMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    // 클라이언트에게 메시지를 보내기 위한 SimpMessagingTemplate은 그대로 둡니다.
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 클라이언트로부터 받은 음성 데이터 조각(chunk)을 처리합니다.
     * 이 메서드는 이제 WebSocketController를 통해 호출됩니다.
     * @param message "audio_chunk" 타입의 메시지
     */
    public void processAudioChunk(VoiceMessage message) {
        log.info("Processing 'audio_chunk' for session: {}", message.getSessionId());

        try {
            byte[] audioData = Base64.getDecoder().decode(message.getData());
            log.info("Decoded audio data size: {} bytes for session: {}", audioData.length, message.getSessionId());

            // TODO: 수신된 audioData를 STT 모델로 전달하고 처리하는 로직
            // ...

            // 예시: 처리 결과를 다시 클라이언트에게 전송
            String destination = "/topic/session/" + message.getSessionId();
            // messagingTemplate.convertAndSend(destination, response);

        } catch (IllegalArgumentException e) {
            log.error("Failed to decode Base64 data for session: {}. Error: {}", message.getSessionId(), e.getMessage());
        }
    }

    /**
     * 클라이언트로부터 받은 텍스트 데이터 조각(chunk)을 처리합니다.
     * @param message "text_chunk" 타입의 메시지
     */
    public void processTextChunk(VoiceMessage message) {
        log.info("Processing 'text_chunk' for session {}: {}", message.getSessionId(), message.getData());
        String userText = message.getData();

        // 1. TODO: [DB 저장] 사용자의 발화 텍스트를 Message 테이블에 저장

        // 2. TODO: [생성형 AI 연동] 수신된 텍스트(userText)를 바탕으로 AI의 답변 생성
        String aiResponseText = "AI가 '" + userText + "'에 대한 답변을 생성했습니다.";

        // 3. TODO: [DB 저장] AI의 답변 내용을 Message 테이블에 저장

        // 4. TODO: [TTS 모델 연동] 생성된 답변 텍스트를 TTS 모델로 보내 음성 데이터 생성
        String encodedAiVoiceData = "BASE64_ENCODED_AI_VOICE_FROM_TEXT_EXAMPLE";

        // 5. 클라이언트에게 AI의 음성 응답 전송
        VoiceMessage response = new VoiceMessage();
        response.setType("ai_voice_chunk");
        response.setData(encodedAiVoiceData);
        response.setSessionId(message.getSessionId());

        String destination = "/topic/session/" + message.getSessionId();
        messagingTemplate.convertAndSend(destination, response);
        log.info("Sent AI voice response (from text) to destination: {}", destination);
    }
}
