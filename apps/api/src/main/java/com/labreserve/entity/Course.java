package com.labreserve.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("courses")
public class Course {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private Long labId;
    private Long teacherId;
    private String semester;
    private Integer dayOfWeek;
    private String startTime;
    private String endTime;
    private LocalDate startDate;
    private LocalDate endDate;
    private String className;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}
