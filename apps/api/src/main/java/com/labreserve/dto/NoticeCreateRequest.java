package com.labreserve.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NoticeCreateRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    private String type;

    private String priority;

    private Long labId;
}
