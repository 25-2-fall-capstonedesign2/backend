package com.capstone.backend.dto;

import com.capstone.backend.entity.VoiceProfile;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class VoiceProfileResponseDto {
    private Long id;
    private String profileName;
    private LocalDateTime createdAt;

    public VoiceProfileResponseDto(VoiceProfile entity) {
        this.id = entity.getId();
        this.profileName = entity.getProfileName();
        this.createdAt = entity.getCreatedAt();
        // 중요: voiceData(이진 데이터)는 포함하지 않습니다!
    }
}