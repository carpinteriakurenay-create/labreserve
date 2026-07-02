package com.labreserve.mapper;

/**
 * Projection row returned by BorrowMapper.aggregateEquipmentUsage.
 */
public class EquipmentUsageAggRow {
    private Long equipmentId;
    private Long borrowCount;
    private Double avgBorrowDays;

    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
    public Long getBorrowCount() { return borrowCount; }
    public void setBorrowCount(Long borrowCount) { this.borrowCount = borrowCount; }
    public Double getAvgBorrowDays() { return avgBorrowDays; }
    public void setAvgBorrowDays(Double avgBorrowDays) { this.avgBorrowDays = avgBorrowDays; }
}
