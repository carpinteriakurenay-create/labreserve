package com.labreserve.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.labreserve.enums.BorrowStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("borrows")
public class Borrow {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long equipmentId;
    private Long userId;
    private LocalDate borrowDate;
    private LocalDate expectedReturn;
    private LocalDate actualReturn;
    private String purpose;
    private BorrowStatus status;
    private String rejectReason;
    private Long approverId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}
