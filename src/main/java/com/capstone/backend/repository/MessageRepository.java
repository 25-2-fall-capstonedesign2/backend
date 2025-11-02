package com.capstone.backend.repository;

import com.capstone.backend.entity.CallSession;
import com.capstone.backend.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long>{
    List<Message> findAllByCallSession(CallSession callSession);
}
