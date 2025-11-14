package com.capstone.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
public class CallService {
    // 대기중인 GPU 워커 세션 풀
    private final Queue<WebSocketSession> availableGpuWorkers = new ConcurrentLinkedQueue<>();
    // 대기중인 고객 세션
    private final Map<Long, WebSocketSession> pendingClients = new ConcurrentHashMap<>();
    // 현재 활성 매칭 (Key: callSessionId, Value: GPU 세션)
    private final Map<Long, WebSocketSession> activePairs = new ConcurrentHashMap<>();
    // 세션 ID <-> 통화 ID/세션 객체 매핑 (빠른 조회를 위함)
    private final Map<String, Long> sessionToCallId = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> callIdToGpuSession = new ConcurrentHashMap<>(); // (callSessionId, GpuSession)
    private final Map<String, WebSocketSession> gpuSessionToClient = new ConcurrentHashMap<>(); // (GpuSessionId, ClientSession)
    private final Map<Long, WebSocketSession> callIdToClientSession = new ConcurrentHashMap<>();
    private final Map<String, Long> gpuSessionToCallId = new ConcurrentHashMap<>(); // [추가] GPU 세션 ID -> 통화 ID (역방향 조회를 위함)
    /**
     * 고객 세션 등록 및 GPU 매칭 시도
     */
    public void registerClient(Long callSessionId, WebSocketSession clientSession) {
        sessionToCallId.put(clientSession.getId(), callSessionId);
        callIdToClientSession.put(callSessionId, clientSession);

        WebSocketSession gpuWorker = availableGpuWorkers.poll(); // 대기 중인 GPU 꺼내기
        if (gpuWorker != null) {
            // 매칭 성공
            activePairs.put(callSessionId, gpuWorker);
            callIdToGpuSession.put(callSessionId.toString(), gpuWorker);
            gpuSessionToClient.put(gpuWorker.getId(), clientSession);
            gpuSessionToCallId.put(gpuWorker.getId(), callSessionId);
            log.info("Client {} paired with GPU {}", clientSession.getId(), gpuWorker.getId());
        } else {
            // 매칭 실패 (대기열 등록)
            pendingClients.put(callSessionId, clientSession);
            log.info("Client {} is pending. No available GPU.", clientSession.getId());
        }
    }

    /**
     * GPU 워커 세션 등록 및 고객 매칭 시도
     */
    public void registerGpu(WebSocketSession gpuSession) {
        // 대기 중인 고객이 있는지 확인
        Map.Entry<Long, WebSocketSession> pendingClientEntry = pendingClients.entrySet().stream().findFirst().orElse(null);

        if (pendingClientEntry != null) {
            // 매칭 성공
            Long callSessionId = pendingClientEntry.getKey();
            WebSocketSession clientSession = pendingClientEntry.getValue();
            pendingClients.remove(callSessionId); // 대기열에서 제거

            activePairs.put(callSessionId, gpuSession);
            callIdToGpuSession.put(callSessionId.toString(), gpuSession);
            gpuSessionToClient.put(gpuSession.getId(), clientSession);
            gpuSessionToCallId.put(gpuSession.getId(), callSessionId);
            log.info("GPU {} paired with pending Client {}", gpuSession.getId(), clientSession.getId());
        } else {
            // 매칭 실패 (대기 풀에 등록)
            availableGpuWorkers.add(gpuSession);
            log.info("GPU {} is available and added to pool.", gpuSession.getId());
        }
    }

    /**
     * 고객 -> GPU로 메시지 전달
     */
    public void forwardToGpu(WebSocketSession clientSession, BinaryMessage message) throws IOException {
        Long callSessionId = sessionToCallId.get(clientSession.getId());
        if (callSessionId == null) return;

        WebSocketSession gpuSession = callIdToGpuSession.get(callSessionId.toString());
        if (gpuSession != null && gpuSession.isOpen()) {
            gpuSession.sendMessage(message);
        } else {
            log.warn("No paired GPU session found for client {}", clientSession.getId());
        }
    }

    /**
     * GPU -> 고객으로 메시지 전달
     */
    public void forwardToClient(WebSocketSession gpuSession, BinaryMessage message) throws IOException {
        WebSocketSession clientSession = gpuSessionToClient.get(gpuSession.getId());
        if (clientSession != null && clientSession.isOpen()) {
            clientSession.sendMessage(message);
        } else {
            log.warn("No paired client session found for GPU {}", gpuSession.getId());
        }
    }

    /**
     * 고객 연결 종료 처리
     */
    public void disconnectClient(WebSocketSession clientSession) {
        Long callSessionId = sessionToCallId.remove(clientSession.getId());
        if (callSessionId == null) return;

        callIdToClientSession.remove(callSessionId);
        pendingClients.remove(callSessionId); // 대기열에 있었다면 제거

        WebSocketSession gpuSession = activePairs.remove(callSessionId);
        if (gpuSession != null) {
            callIdToGpuSession.remove(callSessionId.toString());
            gpuSessionToClient.remove(gpuSession.getId());
            gpuSessionToCallId.remove(gpuSession.getId());
            // TODO: GPU 세션에 "클라이언트가 종료됨" 알림 메시지 전송
            // gpuSession.sendMessage(new TextMessage("client_disconnected"));

            // GPU를 다시 유휴 풀로 반환
            availableGpuWorkers.add(gpuSession);
            log.info("Client {} disconnected. GPU {} returned to pool.", clientSession.getId(), gpuSession.getId());
        }
    }

    /**
     * REST API 호출로 통화 강제 종료
     * @param callSessionId 종료할 통화 ID
     */
    public void forceDisconnectSession(Long callSessionId) {
        // 대기열(pending)에 있는지 확인
        WebSocketSession pendingClient = pendingClients.remove(callSessionId);
        if (pendingClient != null) {
            log.info("Pending client for session {} removed.", callSessionId);
            try {
                pendingClient.close(CloseStatus.NORMAL.withReason("Call ended by API"));
            } catch (IOException e) {
                log.warn("Error closing pending client session: {}", e.getMessage());
            }
            return;
        }

        // 활성(active) 상태인지 확인
        WebSocketSession clientSession = callIdToClientSession.get(callSessionId);
        if (clientSession != null) {
            log.info("Forcibly disconnecting active session: {}", callSessionId);
            try {
                // disconnectClient가 내부적으로 GPU 반환 등 정리 작업을 수행합니다.
                disconnectClient(clientSession);
                clientSession.close(CloseStatus.NORMAL.withReason("Call ended by API"));
            } catch (Exception e) {
                log.warn("Error closing active client session: {}", e.getMessage());
            }
        } else {
            log.warn("Tried to disconnect non-existent session: {}", callSessionId);
        }
    }

    /**
     * GPU 연결 종료 처리
     */
    public void disconnectGpu(WebSocketSession gpuSession) {
        availableGpuWorkers.remove(gpuSession); // 유휴 풀에 있었다면 제거

        Long callSessionId = gpuSessionToCallId.remove(gpuSession.getId());

        WebSocketSession clientSession = gpuSessionToClient.remove(gpuSession.getId());
        if (clientSession != null) {
            // GPU가 죽었으므로, 연결된 고객에게도 연결 종료/오류 알림
            sessionToCallId.remove(clientSession.getId()); // 클라이언트 맵 정리
            callIdToClientSession.remove(callSessionId); // 클라이언트 맵 정리

            log.warn("GPU {} disconnected. Notifying client {}.", gpuSession.getId(), clientSession.getId());
            try {
                // TODO: 고객에게 "GPU 오류" 알림 메시지 전송
                // clientSession.sendMessage(new TextMessage("gpu_error"));
                clientSession.close(CloseStatus.SERVER_ERROR.withReason("GPU worker disconnected"));
            } catch (IOException e) {
                log.error("Error closing client session after GPU disconnect: {}", e.getMessage());
            }
        }
    }
    /**
     * [추가] 6. GpuWebSocketHandler가 호출할 헬퍼 메서드
     * GPU 세션 ID로 현재 통화 ID를 조회합니다.
     *
     * @param gpuSessionId WebSocket 세션의 고유 ID
     * @return 매칭된 callSessionId, 없으면 null
     */
    public Long getCallSessionIdByGpuSession(String gpuSessionId) {
        return gpuSessionToCallId.get(gpuSessionId);
    }
}