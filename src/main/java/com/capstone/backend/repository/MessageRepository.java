package com.capstone.backend.repository;

import com.capstone.backend.entity.CallSession;
import com.capstone.backend.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long>{
    List<Message> findAllByCallSession(CallSession callSession);

    // [추가] 여러 세션(List)에 포함된 모든 메시지를 시간순으로 조회
    List<Message> findAllByCallSessionInOrderByTimestampAsc(List<CallSession> sessions);
}
