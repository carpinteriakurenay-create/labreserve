package com.labreserve.mapper;

/**
 * Projection row returned by BookingMapper.aggregateUserRanking.
 */
public class UserRankingAggRow {
    private Long userId;
    private Long bookingCount;
    private Double totalHours;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getBookingCount() { return bookingCount; }
    public void setBookingCount(Long bookingCount) { this.bookingCount = bookingCount; }
    public Double getTotalHours() { return totalHours; }
    public void setTotalHours(Double totalHours) { this.totalHours = totalHours; }
}
