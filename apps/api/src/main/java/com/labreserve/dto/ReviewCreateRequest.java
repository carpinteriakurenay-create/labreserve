package com.labreserve.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewCreateRequest {

    @NotNull(message = "预约ID不能为空")
    private Long bookingId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低1分")
    @Max(value = 5, message = "评分最高5分")
    private Integer rating;

    @Size(max = 1000, message = "评价内容长度不能超过1000个字符")
    private String comment;
}
