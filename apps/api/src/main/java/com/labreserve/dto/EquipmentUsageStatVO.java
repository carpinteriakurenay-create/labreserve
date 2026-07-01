package com.labreserve.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentUsageStatVO {
    private Long equipmentId;
    private String equipmentName;
    private long borrowCount;
    private double avgBorrowDays;
}
