package com.labreserve.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BookingCreateRequest {

    @NotNull(message = "实验室ID不能为空")
    private Long labId;

    @NotBlank(message = "日期不能为空")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "日期格式为yyyy-MM-dd")
    private String date;

    @NotBlank(message = "开始时间不能为空")
    @Pattern(regexp = "\\d{2}:\\d{2}", message = "时间格式为HH:mm")
    private String startTime;

    @NotBlank(message = "结束时间不能为空")
    @Pattern(regexp = "\\d{2}:\\d{2}", message = "时间格式为HH:mm")
    private String endTime;

    @NotBlank(message = "预约用途不能为空")
    @Size(max = 500, message = "用途描述长度不能超过500个字符")
    private String purpose;

    @Min(value = 1, message = "人数至少为1")
    private Integer personCount;
}
