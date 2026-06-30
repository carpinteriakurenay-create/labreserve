package com.labreserve.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserInfo {
    private Long id;
    private String username;
    private String realName;
    private String role;
    private String avatar;
    private String email;
    private String phone;
    private Boolean enabled;
}
