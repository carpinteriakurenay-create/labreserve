package com.labreserve.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardKpiVO {
    private long todayBookings;
    private long todayBorrows;
    private double labUsageRate;
    private long pendingApprovals;
}
