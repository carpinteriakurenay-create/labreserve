package com.labreserve.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String token;
    private Long expiresIn;
    private UserInfo user;
}
