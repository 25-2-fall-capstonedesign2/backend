package com.capstone.backend.service;

import com.capstone.backend.dto.VoiceMessage;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.util.concurrent.CompletableFuture;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
public class WebSocketService {

    // 로컬 GPU 서버의 ngrok 주소 (실제 주소로 변경해야 합니다)
    private static final String GPU_SERVER_URL = "ws://your-ngrok-address.ngrok.io/ws-stomp";

    private WebSocketStompClient stompClient;
    private StompSession stompSession;

    public WebSocketService() {
        // StandardWebSocketClient를 기본으로 사용하고, SockJS로 폴백할 수 있도록 설정합니다.
        List<Transport> transports = Collections.singletonList(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);
        this.stompClient = new WebSocketStompClient(sockJsClient);
        this.stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    // Spring 컨테이너가 생성된 후 자동으로 연결을 시도합니다.
    @PostConstruct
    public void connect() {
        // CHANGED: ListenableFuture 대신 CompletableFuture를 사용합니다.
        // connectAsync 메서드는 Spring 6.1+ 부터 사용 가능하며, 이전 버전에서는 connect()가 CompletableFuture를 반환합니다.
        CompletableFuture<StompSession> future = stompClient.connectAsync(GPU_SERVER_URL, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                log.info("로컬 GPU 서버와 연결되었습니다. Session: {}", session.getSessionId());
                session.subscribe("/topic/results", this);
                log.info("/topic/results 구독 시작");
            }

            @Override
            public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                log.error("STOMP 예외 발생", exception);
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                log.error("전송 오류 발생: {}", exception.getMessage());
                // 여기서 재연결 로직을 시도할 수 있습니다.
            }

            @Override
            public Type getPayloadType(StompHeaders headers) {
                return VoiceMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                VoiceMessage voiceMessage = (VoiceMessage) payload;
                log.info("GPU 서버로부터 메시지 수신: {}", voiceMessage.getData());
                // TODO: 여기서 SimpMessagingTemplate을 사용하여 특정 클라이언트에게 메시지 전송
            }
        });

        // 비동기 콜백으로 세션과 예외를 처리합니다.
        future.whenComplete((session, throwable) -> {
            if (throwable != null) {
                log.error("로컬 GPU 서버 연결 실패", throwable);
                // TODO: 연결 실패 시 재시도 로직 구현
            } else {
                // 연결 성공 시 세션을 클래스 필드에 저장합니다.
                this.stompSession = session;
                log.info("비동기 연결 성공. 세션 저장 완료.");
            }
        });
    }

    // 애플리케이션 종료 시 연결을 해제합니다.
    @PreDestroy
    public void disconnect() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
            log.info("로컬 GPU 서버와 연결을 해제했습니다.");
        }
    }

    /**
     * 안드로이드 클라이언트로부터 받은 음성 데이터를 GPU 서버로 전송합니다.
     * @param voiceMessage 음성 메시지 DTO
     */
    public void sendToGpuServer(VoiceMessage voiceMessage) {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.send("/app/process-audio", voiceMessage);
            log.info("음성 데이터를 GPU 서버로 전송했습니다. SessionId: {}", voiceMessage.getSessionId());
        } else {
            log.warn("GPU 서버에 연결되어 있지 않아 메시지를 전송할 수 없습니다.");
            // TODO: 연결이 끊겼을 경우, 메시지를 큐에 임시 저장하고 재연결 후 전송하는 로직 추가 가능
        }
    }
}