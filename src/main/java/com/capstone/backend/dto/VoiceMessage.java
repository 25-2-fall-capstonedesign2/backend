package com.capstone.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VoiceMessage {
    private String type; // "audio_chunk", "text_chunk" 로 분류
    private byte[] data; // 실제 데이터 (인코딩 되지 않은 binary 데이터)
    private String sessionId; // 통화 세션 ID
}
