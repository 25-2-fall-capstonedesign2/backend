package com.capstone.backend.service;

import com.capstone.backend.entity.CallSession;
import com.capstone.backend.entity.Message;
import com.capstone.backend.entity.Sender;
import com.capstone.backend.repository.CallSessionRepository;
import com.capstone.backend.repository.MessageRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {
    private final MessageRepository messageRepository;
    private final CallSessionRepository callSessionRepository; // CallSession 프록시를 가져오기 위해 필요

    /**
     * GPU 핸들러로부터 받은 텍스트 메시지를 DB에 저장합니다.
     *
     * @param callSessionId 메시지가 속한 통화 ID
     * @param senderString  발화자 ("USER" 또는 "AI") - 바이너리 프로토콜에서 받은 문자열
     * @param content       메시지 내용 (텍스트)
     */
    @Transactional // DB 쓰기 작업이므로 트랜잭션 적용
    public void saveMessage(Long callSessionId, String senderString, String content) {

        try {
            // 1. (성능 최적화)
            // CallSession 엔티티를 DB에서 SELECT 해오는 대신,
            // getReferenceById를 사용해 ID만 가진 '프록시(껍데기)' 객체를 가져옵니다.
            // Message 저장 시 FK(ID)만 필요하므로 DB 조회를 1번 아낄 수 있습니다.
            CallSession callSession = callSessionRepository.getReferenceById(callSessionId);

            // 2. (중요) String -> Sender Enum 변환
            // GpuWebSocketHandler는 String("USER" or "AI")을 주지만,
            // Message 엔티티는 Sender Enum 타입을 받으므로 변환이 필요합니다.
            Sender sender = Sender.valueOf(senderString.toUpperCase()); // "USER" -> Sender.USER

            // 3. Message 엔티티 생성 (엔티티의 @Builder 사용)
            Message message = Message.builder()
                    .callSession(callSession)
                    .sender(sender)
                    .content(content)
                    .build(); // timestamp는 @CreationTimestamp가 자동 처리

            // 4. DB에 저장
            messageRepository.save(message);

        } catch (EntityNotFoundException e) {
            // getReferenceById는 해당 ID가 DB에 없을 때, 실제 사용하려 하면 이 예외가 발생합니다.
            log.warn("Failed to save message: CallSession not found with ID: {}", callSessionId);
        } catch (IllegalArgumentException e) {
            // Sender.valueOf()가 "USER", "AI" 외의 값을 받을 때 발생합니다.
            log.warn("Failed to save message: Invalid sender type '{}'", senderString, e);
        } catch (Exception e) {
            // 기타 모든 예외
            log.error("Error saving message for session {}: {}", callSessionId, e.getMessage(), e);
        }
    }
}
