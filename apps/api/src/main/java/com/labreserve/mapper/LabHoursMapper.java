package com.labreserve.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labreserve.entity.LabHours;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LabHoursMapper extends BaseMapper<LabHours> {

    @Delete("DELETE FROM lab_hours WHERE lab_id = #{labId}")
    void hardDeleteByLabId(@Param("labId") Long labId);
}
