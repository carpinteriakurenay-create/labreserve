package com.labreserve.mapper;

/**
 * Projection row returned by BookingMapper.aggregateLabUsage.
 */
public class LabUsageAggRow {
    private Long labId;
    private Long bookingCount;
    private Double usageHours;

    public Long getLabId() { return labId; }
    public void setLabId(Long labId) { this.labId = labId; }
    public Long getBookingCount() { return bookingCount; }
    public void setBookingCount(Long bookingCount) { this.bookingCount = bookingCount; }
    public Double getUsageHours() { return usageHours; }
    public void setUsageHours(Double usageHours) { this.usageHours = usageHours; }
}
