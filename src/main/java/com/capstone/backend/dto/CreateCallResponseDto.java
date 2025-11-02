package com.capstone.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor // 생성자를 통해 값을 받습니다.
public class CreateCallResponseDto {
    private Long callSessionId;
}