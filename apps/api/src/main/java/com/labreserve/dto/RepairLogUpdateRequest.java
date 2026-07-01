package com.labreserve.dto;

import com.labreserve.enums.RepairStatus;
import lombok.Data;

@Data
public class RepairLogUpdateRequest {
    private String description;
    private RepairStatus status;
}
