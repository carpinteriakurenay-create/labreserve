package com.labreserve.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BookingUpdateRequest {

    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "日期格式为yyyy-MM-dd")
    private String date;

    @Pattern(regexp = "\\d{2}:\\d{2}", message = "时间格式为HH:mm")
    private String startTime;

    @Pattern(regexp = "\\d{2}:\\d{2}", message = "时间格式为HH:mm")
    private String endTime;

    @Size(max = 500, message = "用途描述长度不能超过500个字符")
    private String purpose;

    @Min(value = 1, message = "人数至少为1")
    private Integer personCount;
}
