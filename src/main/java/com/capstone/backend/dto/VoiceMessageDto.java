package com.capstone.backend.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class VoiceMessageDto {
    private String type;            // "start", "audio", "end" 등
    private String sessionId;       // 통화 세션 ID (String으로 통일 권장)
    private Long voiceProfileId;    // [필수] GPU가 사용할 목소리 ID
    private Long userId;          // 세션에 연결된 유저 id
    private String data;            // 오디오 데이터 (start 메시지엔 null)
}