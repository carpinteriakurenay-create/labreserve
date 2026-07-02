package com.labreserve.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labreserve.entity.Booking;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BookingMapper extends BaseMapper<Booking> {

    /** Count distinct lab IDs with completed bookings on a given date. */
    @Select("SELECT COUNT(DISTINCT lab_id) FROM bookings WHERE date = #{date} AND status = 'COMPLETED' AND deleted = 0")
    long countDistinctLabsByDate(@Param("date") String date);

    /**
     * Aggregate completed booking counts grouped by lab.
     * Uses a simple COUNT/GROUP BY query compatible with both H2 and MySQL.
     * Time-based aggregation is done in Java for cross-DB compatibility.
     */
    @Select("""
        SELECT b.lab_id, COUNT(*) AS booking_count
        FROM bookings b
        WHERE b.status = 'COMPLETED' AND b.date >= #{dateFrom} AND b.date <= #{dateTo}
              AND b.deleted = 0
        GROUP BY b.lab_id
        ORDER BY booking_count DESC
        """)
    List<LabUsageAggRow> aggregateLabUsage(@Param("dateFrom") String dateFrom, @Param("dateTo") String dateTo);

    /**
     * Aggregate completed booking counts grouped by user (top N).
     * Time-based aggregation is done in Java for cross-DB compatibility.
     */
    @Select("""
        SELECT b.user_id, COUNT(*) AS booking_count
        FROM bookings b
        WHERE b.status = 'COMPLETED' AND b.date >= #{dateFrom} AND b.date <= #{dateTo}
              AND b.deleted = 0
        GROUP BY b.user_id
        ORDER BY booking_count DESC
        LIMIT #{limit}
        """)
    List<UserRankingAggRow> aggregateUserRanking(@Param("dateFrom") String dateFrom,
                                                  @Param("dateTo") String dateTo,
                                                  @Param("limit") int limit);
}
