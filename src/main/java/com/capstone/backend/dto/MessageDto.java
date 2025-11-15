package com.capstone.backend.dto;

import com.capstone.backend.entity.Message;
import com.capstone.backend.entity.Sender;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MessageDto {
    private Long messageId;
    private Sender sender;
    private String content;
    private LocalDateTime timestamp;

    // 엔티티를 DTO로 변환하는 생성자
    public MessageDto(Message message) {
        this.messageId = message.getId();
        this.sender = message.getSender();
        this.content = message.getContent();
        this.timestamp = message.getTimestamp();
    }
}
