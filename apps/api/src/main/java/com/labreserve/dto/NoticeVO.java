package com.labreserve.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeVO {

    private Long id;
    private String title;
    private String content;
    private String type;
    private String priority;
    private Long publisherId;
    private String publisherName;
    private Long labId;
    private String labName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
