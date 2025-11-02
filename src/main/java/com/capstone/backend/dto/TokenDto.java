package com.capstone.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

// 명세서 응답 바디 형식 [cite: 133]
@Getter
@Setter
@AllArgsConstructor
public class TokenDto {
    private String token;
}
