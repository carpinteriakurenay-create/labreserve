package com.labreserve.dto;

import lombok.Data;

@Data
public class CourseUpdateRequest {

    private String name;
    private Long labId;
    private Long teacherId;
    private Integer dayOfWeek;
    private String startTime;
    private String endTime;
    private String startDate;
    private String endDate;
    private String className;
}
