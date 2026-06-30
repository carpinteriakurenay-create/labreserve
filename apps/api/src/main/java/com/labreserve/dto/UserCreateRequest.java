package com.labreserve.dto;

import com.labreserve.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserCreateRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度为3-50个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度为6-100个字符")
    private String password;

    @NotBlank(message = "姓名不能为空")
    @Size(min = 1, max = 50, message = "姓名长度为1-50个字符")
    private String realName;

    private UserRole role;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String phone;
}
