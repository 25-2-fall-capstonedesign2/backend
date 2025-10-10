package com.capstone.backend.service;

import com.capstone.backend.dto.VoiceMessage;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    // TODO: AI 모델이 실행되는 로컬 PC의 ngrok 주소를 여기에 입력해야 합니다.
    private static final String AI_SERVER_URL = "ws://your-ngrok-address.ngrok.io/ws-stomp";

    private final SimpMessagingTemplate messagingTemplate;
    private WebSocketStompClient stompClient;
    private StompSession stompSession;

    // 서버 시작 시 연결 시도를 해제 -- 테스트를 위한 주석 처리 위치
    //@PostConstruct
    public void init() {
        // WebSocketStompClient를 초기화합니다.
        List<Transport> transports = Collections.singletonList(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);
        this.stompClient = new WebSocketStompClient(sockJsClient);
        this.stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        // 애플리케이션 시작 시 AI 서버와 WebSocket 연결을 시도합니다.
        connectToAiServer();
    }

    public void connectToAiServer() {
        try {
            CompletableFuture<StompSession> future = stompClient.connectAsync(AI_SERVER_URL, new StompSessionHandlerAdapter() {
                @Override
                public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                    log.info("AI 모델 서버와 성공적으로 연결되었습니다. Session ID: {}", session.getSessionId());
                    // AI 서버로부터 결과를 받을 주소를 구독합니다.
                    session.subscribe("/topic/results", this);
                }

                @Override
                public void handleTransportError(StompSession session, Throwable exception) {
                    log.error("AI 모델 서버 연결 오류 발생: {}", exception.getMessage());
                    // TODO: 재연결 로직을 여기에 추가할 수 있습니다.
                }

                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return VoiceMessage.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    // AI 서버로부터 처리 결과를 받으면 이 메서드가 호출됩니다.
                    VoiceMessage resultMessage = (VoiceMessage) payload;
                    log.info("AI 모델 서버로부터 결과 수신 (Session ID: {})", resultMessage.getSessionId());

                    // 메시지에 담긴 세션 ID를 기반으로, 결과를 요청했던 특정 클라이언트에게 메시지를 보냅니다.
                    String destination = "/topic/response/" + resultMessage.getSessionId();
                    messagingTemplate.convertAndSend(destination, resultMessage);
                }
            });

            future.whenComplete((session, throwable) -> {
                if (throwable != null) {
                    log.error("AI 모델 서버 연결에 최종 실패했습니다.", throwable);
                } else {
                    this.stompSession = session;
                }
            });
        } catch (Exception e) {
            log.error("STOMP 연결 과정에서 예외가 발생했습니다.", e);
        }
    }

    /**
     * 컨트롤러로부터 호출되어, 받은 음성 데이터를 AI 서버로 전송합니다.
     * @param message 클라이언트로부터 받은 음성 메시지
     */
    public void sendToAiModel(VoiceMessage message) {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.send("/app/process", message);
            log.info("세션 [{}]의 메시지를 AI 서버로 전달했습니다.", message.getSessionId());
        } else {
            log.warn("AI 모델 서버에 연결되지 않아 메시지를 전송할 수 없습니다.");
        }
    }

    /**
     * 클라이언트로부터 받은 음성 데이터 조각(chunk)을 처리합니다.
     * @param message "audio_chunk" 타입의 메시지
     */
    public void processAudioChunk(VoiceMessage message) {
        log.info("Processing 'audio_chunk' for session: {}", message.getSessionId());

        try {
            // 1. Base64로 인코딩된 data를 byte[]로 디코딩
            byte[] audioData = Base64.getDecoder().decode(message.getData());
            log.info("Decoded audio data size: {} bytes for session: {}", audioData.length, message.getSessionId());

            // 2. TODO: [AI STT 모델 연동] 디코딩된 audioData를 STT 모델로 전송
            // ... (이하 로직은 이전과 동일)

            // (이하 생략)
            String destination = "/topic/session/" + message.getSessionId();
            // messagingTemplate.convertAndSend(destination, response);

        } catch (IllegalArgumentException e) {
            log.error("Failed to decode Base64 data for session: {}. Error: {}", message.getSessionId(), e.getMessage());
        }
    }

    /**
     * 클라이언트로부터 받은 텍스트 데이터 조각(chunk)을 처리합니다.
     * (예: STT가 클라이언트 단에서 수행되었거나, 디버깅 목적으로 텍스트를 보낼 경우)
     * @param message "text_chunk" 타입의 메시지
     */
    public void processTextChunk(VoiceMessage message) {
        log.info("Processing 'text_chunk' for session {}: {}", message.getSessionId(), message.getData());
        String userText = message.getData();

        // 1. TODO: [DB 저장] 사용자의 발화 텍스트를 Message 테이블에 저장
        //    (messageRepository.save(...) 호출)

        // 2. TODO: [생성형 AI 연동] 수신된 텍스트(userText)를 바탕으로 AI의 답변 생성
        String aiResponseText = "생성형 AI가 '" + userText + "'에 대한 답변을 생성했습니다.";

        // 3. TODO: [DB 저장] AI의 답변 내용을 Message 테이블에 저장

        // 4. TODO: [TTS 모델 연동] 생성된 답변 텍스트를 TTS 모델로 보내 음성 데이터 생성
        //    생성된 음성 데이터를 Base64 문자열로 인코딩했다고 가정
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

    @PreDestroy
    public void disconnect() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
            log.info("AI 모델 서버와 연결을 종료했습니다.");
        }
    }
}
