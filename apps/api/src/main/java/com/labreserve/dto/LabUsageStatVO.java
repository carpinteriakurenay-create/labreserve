package com.labreserve.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabUsageStatVO {
    private Long labId;
    private String labName;
    private long bookingCount;
    private double usageHours;
    private double utilizationRate;
}
