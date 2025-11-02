package com.capstone.backend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor // STOMP는 JSON <-> 객체 변환 시 기본 생성자를 사용합니다.
@AllArgsConstructor
public class TextMessage {
    private String type; // "transcript", "command", "user_message" 등
    private String sender; // "USER", "AI"
    private String content; // 실제 텍스트 데이터
    private String sessionId; // 통화 세션 ID
}
