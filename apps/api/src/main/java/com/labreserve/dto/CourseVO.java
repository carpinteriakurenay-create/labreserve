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
public class CourseVO {

    private Long id;
    private String name;
    private Long labId;
    private String labName;
    private Long teacherId;
    private String teacherName;
    private String semester;
    private Integer dayOfWeek;
    private String startTime;
    private String endTime;
    private String startDate;
    private String endDate;
    private String className;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
