package com.capstone.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateCallRequestDto {

    // Flutter에서 {"participantName": "Karina"} 이렇게 보낼 겁니다.
    private Long voiceProfileId;
}
