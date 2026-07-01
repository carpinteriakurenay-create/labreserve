package com.labreserve.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BorrowCreateRequest {

    @NotNull(message = "设备ID不能为空")
    private Long equipmentId;

    @NotBlank(message = "借用日期不能为空")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "日期格式为yyyy-MM-dd")
    private String borrowDate;

    @NotBlank(message = "预计归还日期不能为空")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "日期格式为yyyy-MM-dd")
    private String expectedReturn;

    @NotBlank(message = "借用用途不能为空")
    @Size(max = 500, message = "用途描述长度不能超过500个字符")
    private String purpose;
}
