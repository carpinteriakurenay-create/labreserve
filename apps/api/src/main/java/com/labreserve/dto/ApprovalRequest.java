package com.labreserve.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApprovalRequest {

    private boolean approved;

    @Size(max = 500, message = "拒绝原因长度不能超过500个字符")
    private String rejectReason;
}
