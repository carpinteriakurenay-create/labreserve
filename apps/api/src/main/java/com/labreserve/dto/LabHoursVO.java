package com.labreserve.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabHoursVO {
    private Long id;
    private Long labId;
    private Integer dayOfWeek;
    private String openTime;
    private String closeTime;
}
