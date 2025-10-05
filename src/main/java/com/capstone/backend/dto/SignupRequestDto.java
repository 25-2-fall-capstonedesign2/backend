package com.capstone.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequestDto {
    private String phone;
    private String password;
    private String displayName;
}