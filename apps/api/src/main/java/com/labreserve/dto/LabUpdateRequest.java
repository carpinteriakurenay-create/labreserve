package com.labreserve.dto;

import com.labreserve.enums.LabStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LabUpdateRequest {

    @Size(min = 1, max = 100, message = "实验室名称长度为1-100个字符")
    private String name;

    @Size(max = 200, message = "位置描述长度不能超过200个字符")
    private String location;

    @Min(value = 1, message = "容量至少为1")
    private Integer capacity;

    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;

    private String imageUrl;
    private LabStatus status;
    private Long managerId;
}
