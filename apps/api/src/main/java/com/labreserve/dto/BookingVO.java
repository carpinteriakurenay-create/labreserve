package com.labreserve.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingVO {
    private Long id;
    private Long labId;
    private String labName;
    private Long userId;
    private String userName;
    private LocalDate date;
    private String startTime;
    private String endTime;
    private String purpose;
    private Integer personCount;
    private String status;
    private String rejectReason;
    private Long approverId;
    private String approverName;
    private LocalDateTime approvedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
