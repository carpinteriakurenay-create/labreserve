package com.labreserve.dto;

import com.labreserve.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateRequest {

    @Size(min = 1, max = 50, message = "姓名长度为1-50个字符")
    private String realName;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String phone;

    private UserRole role;

    private Boolean enabled;
}
