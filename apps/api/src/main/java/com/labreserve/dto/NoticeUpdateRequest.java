package com.labreserve.dto;

import lombok.Data;

@Data
public class NoticeUpdateRequest {

    private String title;

    private String content;

    private String type;

    private String priority;

    private Long labId;
}
