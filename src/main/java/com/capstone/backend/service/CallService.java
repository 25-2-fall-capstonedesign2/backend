package com.capstone.backend.service;

import com.capstone.backend.dto.VoiceMessageDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallService {

    private final CallSessionService callSessionService; // DB 정보 조회용
    private final ObjectMapper objectMapper; // JSON 변환용

    // 세션 관리용 맵
    private final Queue<WebSocketSession> availableGpuWorkers = new ConcurrentLinkedQueue<>();
    private final Map<Long, WebSocketSession> waitingClients = new ConcurrentHashMap<>(); // 대기 중인 고객
    private final Map<Long, WebSocketSession> activePairs = new ConcurrentHashMap<>();    // 매칭된 쌍 (CallID -> GPU)

    // 세션 ID로 정보 역추적용
    private final Map<String, Long> clientSessionToCallId = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> gpuSessionToClient = new ConcurrentHashMap<>();

    private final Map<String, Long> gpuSessionToCallId = new ConcurrentHashMap<>();
    // =========================================================
    // 1. 고객 접속 처리 (ClientWebSocketHandler에서 호출)
    // =========================================================
    public void registerClient(Long callSessionId, WebSocketSession clientSession) {
        log.info("Registering Client: SessionID={}, CallID={}", clientSession.getId(), callSessionId);

        clientSessionToCallId.put(clientSession.getId(), callSessionId);

        // GPU 매칭 시도
        WebSocketSession gpuSession = availableGpuWorkers.poll();
        if (gpuSession != null) {
            match(callSessionId, clientSession, gpuSession);
        } else {
            waitingClients.put(callSessionId, clientSession);
            log.info("Client waiting for GPU...");
        }
    }

    // =========================================================
    // 2. GPU 접속 처리 (GpuWebSocketHandler에서 호출)
    // =========================================================
    public void registerGpu(WebSocketSession gpuSession) {
        log.info("Registering GPU: SessionID={}", gpuSession.getId());

        // 대기 중인 고객이 있는지 확인
        Long callSessionId = waitingClients.keySet().stream().findFirst().orElse(null);

        if (callSessionId != null) {
            WebSocketSession clientSession = waitingClients.remove(callSessionId);
            if (clientSession.isOpen()) {
                match(callSessionId, clientSession, gpuSession);
            } else {
                // 고객이 기다리다 나갔으면 다시 풀에 등록
                availableGpuWorkers.add(gpuSession);
            }
        } else {
            availableGpuWorkers.add(gpuSession);
            log.info("GPU added to pool. Total GPUs: {}", availableGpuWorkers.size());
        }
    }

    // =========================================================
    // 3. 매칭 로직 (핵심: 여기서 GPU에게 Start 신호를 보냄)
    // =========================================================
    private void match(Long callSessionId, WebSocketSession client, WebSocketSession gpu) {
        activePairs.put(callSessionId, gpu);
        gpuSessionToClient.put(gpu.getId(), client);
        gpuSessionToCallId.put(gpu.getId(), callSessionId);

        log.info("Matched: CallID={} <-> GPU={}", callSessionId, gpu.getId());

        // [DB 조회] 해당 통화가 어떤 목소리인지 가져옴
        Long voiceProfileId = callSessionService.getVoiceProfileId(callSessionId);

        Long userId = callSessionService.getUserId(callSessionId);

        // [전송] GPU에게 "시작해" 메시지 (JSON)
        VoiceMessageDto startMsg = VoiceMessageDto.builder()
                .type("start")
                .sessionId(String.valueOf(callSessionId))
                .voiceProfileId(voiceProfileId)
                .userId(userId)
                .build();

        sendJsonToGpu(gpu, startMsg);
    }

    // =========================================================
    // 4. 메시지 전달 (오디오 스트리밍)
    // =========================================================

    // 고객 -> GPU (오디오 데이터)
    public void forwardAudioToGpu(WebSocketSession clientSession, BinaryMessage message) {
        Long callSessionId = clientSessionToCallId.get(clientSession.getId());
        if (callSessionId != null) {
            WebSocketSession gpu = activePairs.get(callSessionId);
            if (gpu != null && gpu.isOpen()) {
                try {
                    gpu.sendMessage(message);
                } catch (IOException e) {
                    log.error("Failed to forward audio to GPU", e);
                }
            }
        }
    }

    // GPU -> 고객 (변환된 오디오)
    public void forwardAudioToClient(WebSocketSession gpuSession, BinaryMessage message) {
        WebSocketSession client = gpuSessionToClient.get(gpuSession.getId());
        if (client != null && client.isOpen()) {
            try {
                client.sendMessage(message);
            } catch (IOException e) {
                log.error("Failed to forward audio to Client", e);
            }
        }
    }

    // =========================================================
    // 5. 연결 종료 처리
    // =========================================================

    public void clientDisconnected(WebSocketSession clientSession) {
        Long callId = clientSessionToCallId.remove(clientSession.getId());
        if (callId != null) {
            waitingClients.remove(callId); // 대기 중이었다면 제거
            WebSocketSession gpu = activePairs.remove(callId);

            if (gpu != null) {
                gpuSessionToClient.remove(gpu.getId());
                gpuSessionToCallId.remove(gpu.getId());
                // GPU는 재사용을 위해 풀로 반환
                availableGpuWorkers.add(gpu);
                log.info("GPU {} returned to pool", gpu.getId());
            }
            // DB 종료 처리
            callSessionService.endCallSession(callId);
        }
    }

    public void gpuDisconnected(WebSocketSession gpuSession) {
        availableGpuWorkers.remove(gpuSession);

        Long callId = gpuSessionToCallId.remove(gpuSession.getId()); // [추가됨]
        if (callId != null) {
            activePairs.remove(callId);
        }

        WebSocketSession client = gpuSessionToClient.remove(gpuSession.getId());
        if (client != null) {
            // 연결된 고객이 있었다면 에러 알림 후 종료
            try {
                client.close(CloseStatus.SERVER_ERROR.withReason("GPU Disconnected"));
            } catch (IOException e) {/*ignore*/}
        }
    }

    // 유틸리티: GPU에게 JSON 전송
    private void sendJsonToGpu(WebSocketSession gpu, VoiceMessageDto msg) {
        try {
            String json = objectMapper.writeValueAsString(msg);
            gpu.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("Error sending Start signal to GPU", e);
        }
    }

    /**
     * GPU 세션 ID로 현재 진행 중인 CallSessionId를 조회합니다.
     * GpuWebSocketHandler에서 텍스트 메시지 처리 시 사용됩니다.
     */
    public Long getCallSessionIdByGpuSession(String gpuSessionId) {
        return gpuSessionToCallId.get(gpuSessionId);
    }

    // API에서 강제 종료 시 사용
    public void forceDisconnect(Long callSessionId) {
        log.info("Force disconnecting session: {}", callSessionId);

        // 1. 대기열(Waiting)에 있는 경우
        WebSocketSession waitingClient = waitingClients.remove(callSessionId);
        if (waitingClient != null) {
            clientSessionToCallId.remove(waitingClient.getId());
            try {
                waitingClient.close(CloseStatus.NORMAL.withReason("Call ended by API"));
            } catch (IOException e) {
                log.error("Error closing waiting client", e);
            }
            return;
        }

        // 2. 활성(Active) 상태인 경우
        WebSocketSession gpu = activePairs.remove(callSessionId);
        if (gpu != null) {
            // GPU 정리 및 반환
            gpuSessionToCallId.remove(gpu.getId());
            WebSocketSession client = gpuSessionToClient.remove(gpu.getId());
            availableGpuWorkers.add(gpu); // GPU 재사용

            // 클라이언트 종료
            if (client != null) {
                clientSessionToCallId.remove(client.getId());
                try {
                    client.close(CloseStatus.NORMAL.withReason("Call ended by API"));
                } catch (IOException e) {
                    log.error("Error closing active client", e);
                }
            }
        }
    }

    public void notifyClientGpuIsReady(Long callSessionId) {
        WebSocketSession gpuSession = activePairs.get(callSessionId);
        if (gpuSession == null) {
            log.warn("GPU session not found for CallID: {}", callSessionId);
            return;
        }
        WebSocketSession clientSession = gpuSessionToClient.get(gpuSession.getId());

        if (clientSession != null && clientSession.isOpen()) {
            try {
                // 2. 심플한 JSON 생성 (type: system, event: ready)
                // Jackson 라이브러리(ObjectMapper) 사용 가정
                Map<String, String> signal = new HashMap<>();
                signal.put("type", "system");
                signal.put("event", "ready");

                String jsonMessage = new ObjectMapper().writeValueAsString(signal);

                // 3. 클라이언트에게 전송
                clientSession.sendMessage(new TextMessage(jsonMessage));

                log.info("Sent READY signal (type=system, event=ready) to client: {}", callSessionId);

            } catch (IOException e) {
                log.error("Failed to send ready signal", e);
            }
        }
    }
}