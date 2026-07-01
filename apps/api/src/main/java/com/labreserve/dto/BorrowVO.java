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
public class BorrowVO {
    private Long id;
    private Long equipmentId;
    private String equipmentName;
    private Long userId;
    private String userName;
    private LocalDate borrowDate;
    private LocalDate expectedReturn;
    private LocalDate actualReturn;
    private String purpose;
    private String status;
    private String rejectReason;
    private Long approverId;
    private String approverName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
