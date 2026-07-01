package com.labreserve.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RepairLogCreateRequest {
    @NotNull(message = "设备不能为空")
    private Long equipmentId;

    @NotBlank(message = "故障描述不能为空")
    private String description;
}
