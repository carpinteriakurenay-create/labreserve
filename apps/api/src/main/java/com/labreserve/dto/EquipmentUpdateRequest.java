package com.labreserve.dto;

import com.labreserve.enums.EquipmentStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EquipmentUpdateRequest {

    private Long labId;

    @Size(min = 1, max = 100, message = "设备名称长度为1-100个字符")
    private String name;

    @Size(max = 100, message = "型号长度不能超过100个字符")
    private String model;

    @Size(min = 1, max = 100, message = "序列号长度为1-100个字符")
    private String serialNumber;

    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;

    private EquipmentStatus status;
}
