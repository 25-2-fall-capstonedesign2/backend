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

    @PostConstruct
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
            stompSession.send("/app/process-audio", message);
        } else {
            log.warn("AI 모델 서버에 연결되지 않아 메시지를 전송할 수 없습니다.");
        }
    }

    @PreDestroy
    public void disconnect() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
            log.info("AI 모델 서버와 연결을 종료했습니다.");
        }
    }
}
