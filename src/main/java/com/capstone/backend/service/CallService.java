package com.capstone.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class CallService {

    // 전체 활성 세션을 관리 (세션 ID, 세션 객체)
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    // 고객 세션과 GPU 워커 세션을 1:1로 매핑 (고객 세션 ID, GPU 세션 ID)
    private final Map<String, String> sessionPairs = new ConcurrentHashMap<>();
    // 반대 방향 매핑 (GPU 세션 ID, 고객 세션 ID)
    private final Map<String, String> reverseSessionPairs = new ConcurrentHashMap<>();

    /**
     * 새로운 WebSocket 연결을 등록합니다.
     */
    public void registerSession(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.info("Session registered: {}", session.getId());

        // TODO: 세션을 고객과 GPU로 구분하고 페어링하는 로직이 필요합니다.
        // 예: URI 쿼리 파라미터로 ?role=customer 또는 ?role=gpu 등으로 구분
        // 지금은 임시로 고객과 GPU를 번갈아 가며 페어링하는 로직을 가정해 봅니다.
        findAndPairPartner(session);
    }

    /**
     * 연결된 세션을 기반으로 메시지를 상대방에게 전달합니다.
     */
    public void forwardMessage(WebSocketSession session, BinaryMessage message) {
        String sessionId = session.getId();
        String partnerSessionId = sessionPairs.get(sessionId); // 내가 고객일 경우
        if (partnerSessionId == null) {
            partnerSessionId = reverseSessionPairs.get(sessionId); // 내가 GPU일 경우
        }

        if (partnerSessionId != null) {
            WebSocketSession partnerSession = sessions.get(partnerSessionId);
            if (partnerSession != null && partnerSession.isOpen()) {
                try {
                    partnerSession.sendMessage(message);
                    log.info("Message forwarded from {} to {}", sessionId, partnerSessionId);
                } catch (IOException e) {
                    log.error("Failed to forward message from {} to {}: {}", sessionId, partnerSessionId, e.getMessage());
                }
            } else {
                log.warn("Partner session {} for {} is not available.", partnerSessionId, sessionId);
            }
        } else {
            log.warn("No partner found for session {}. Message not forwarded.", sessionId);
        }
    }

    /**
     * WebSocket 연결 종료 시 세션 정보를 정리합니다.
     */
    public void removeSession(WebSocketSession session) {
        String sessionId = session.getId();
        sessions.remove(sessionId);

        // 내가 고객이라면, 매핑된 GPU 정보도 제거
        String gpuSessionId = sessionPairs.remove(sessionId);
        if (gpuSessionId != null) {
            reverseSessionPairs.remove(gpuSessionId);
            log.info("Pairing removed for customer: {}", sessionId);
        }

        // 내가 GPU라면, 매핑된 고객 정보도 제거
        String customerSessionId = reverseSessionPairs.remove(sessionId);
        if (customerSessionId != null) {
            sessionPairs.remove(customerSessionId);
            log.info("Pairing removed for GPU: {}", sessionId);
        }

        log.info("Session removed: {}", sessionId);
    }

    // 임시 페어링 로직
    private void findAndPairPartner(WebSocketSession newSession) {
        // 현재 파트너가 없는 다른 세션을 찾아 페어링합니다.
        // 실제로는 대기 중인 GPU를 찾거나 하는 더 정교한 로직이 필요합니다.
        sessions.values().stream()
                .filter(s -> !s.getId().equals(newSession.getId())) // 자기 자신 제외
                .filter(s -> !sessionPairs.containsKey(s.getId()) && !reverseSessionPairs.containsKey(s.getId())) // 파트너가 없는 세션
                .findFirst()
                .ifPresent(partner -> {
                    // newSession을 고객, partner를 GPU로 가정
                    sessionPairs.put(newSession.getId(), partner.getId());
                    reverseSessionPairs.put(partner.getId(), newSession.getId());
                    log.info("Session {} and {} are paired.", newSession.getId(), partner.getId());
                });
    }
}