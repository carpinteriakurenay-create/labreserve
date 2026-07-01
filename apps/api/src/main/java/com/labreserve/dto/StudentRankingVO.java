package com.labreserve.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentRankingVO {
    private Long userId;
    private String userRealName;
    private long bookingCount;
    private double totalHours;
}
