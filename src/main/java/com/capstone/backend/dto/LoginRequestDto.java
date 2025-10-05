package com.capstone.backend.dto;

import lombok.Getter;
import lombok.Setter;

// 명세서 요청 바디 형식 [cite: 129]
@Getter
@Setter
public class LoginRequestDto {
    private String phone;
    private String password;
}
