package com.labreserve.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StudentCreateRequest {
    @NotNull(message = "所属实验室不能为空")
    private Long labId;

    @NotBlank(message = "姓名不能为空")
    private String name;

    private String gender;
    private Integer age;
    private String address;
}
