package com.labreserve.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CourseCreateRequest {

    @NotBlank(message = "课程名称不能为空")
    private String name;

    @NotNull(message = "实验室不能为空")
    private Long labId;

    @NotNull(message = "任课教师不能为空")
    private Long teacherId;

    @NotBlank(message = "学期不能为空")
    private String semester;

    @NotNull(message = "星期不能为空")
    private Integer dayOfWeek;

    @NotBlank(message = "开始时间不能为空")
    private String startTime;

    @NotBlank(message = "结束时间不能为空")
    private String endTime;

    @NotBlank(message = "开始日期不能为空")
    private String startDate;

    @NotBlank(message = "结束日期不能为空")
    private String endDate;

    @NotBlank(message = "班级不能为空")
    private String className;
}
