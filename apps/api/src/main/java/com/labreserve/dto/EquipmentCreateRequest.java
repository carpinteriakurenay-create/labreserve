package com.labreserve.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EquipmentCreateRequest {

    @NotNull(message = "实验室ID不能为空")
    private Long labId;

    @NotBlank(message = "设备名称不能为空")
    @Size(min = 1, max = 100, message = "设备名称长度为1-100个字符")
    private String name;

    @Size(max = 100, message = "型号长度不能超过100个字符")
    private String model;

    @NotBlank(message = "序列号不能为空")
    @Size(min = 1, max = 100, message = "序列号长度为1-100个字符")
    private String serialNumber;

    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;
}
