package com.capstone.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VoiceMessage {
    private String type; // "audio_chunk", "command", "result" 등 메시지 종류
    private String data; // 실제 데이터 (Base64 인코딩된 오디오 데이터 등)
    private String sessionId; // 통화 세션 ID
}
