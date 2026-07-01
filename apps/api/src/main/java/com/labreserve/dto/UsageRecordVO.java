package com.labreserve.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageRecordVO {
    private Long id;
    private Long bookingId;
    private Long labId;
    private String labName;
    private Long userId;
    private String userRealName;
    private String date;
    private String startTime;
    private String endTime;
    private String purpose;
    private Integer personCount;
    private String completedAt;
}
