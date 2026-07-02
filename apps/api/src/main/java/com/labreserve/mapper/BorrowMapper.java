package com.labreserve.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labreserve.entity.Borrow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BorrowMapper extends BaseMapper<Borrow> {

    /**
     * Aggregate borrow counts grouped by equipment.
     * Day-based average is done in Java for cross-DB compatibility.
     */
    @Select("""
        SELECT b.equipment_id, COUNT(*) AS borrow_count
        FROM borrows b
        WHERE b.borrow_date >= #{dateFrom} AND b.borrow_date <= #{dateTo}
              AND b.deleted = 0 AND b.actual_return IS NOT NULL
        GROUP BY b.equipment_id
        ORDER BY borrow_count DESC
        """)
    List<EquipmentUsageAggRow> aggregateEquipmentUsage(@Param("dateFrom") String dateFrom,
                                                        @Param("dateTo") String dateTo);
}
