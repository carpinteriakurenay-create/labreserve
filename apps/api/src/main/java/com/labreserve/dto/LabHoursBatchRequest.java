package com.labreserve.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
public class LabHoursBatchRequest {

    @NotEmpty(message = "开放时间不能为空")
    @Valid
    private List<LabHoursItem> hours;

    @Data
    public static class LabHoursItem {

        @NotNull(message = "星期不能为空")
        @Min(value = 1, message = "星期取值范围为1-7")
        @Max(value = 7, message = "星期取值范围为1-7")
        private Integer dayOfWeek;

        @NotBlank(message = "开始时间不能为空")
        @Pattern(regexp = "^\\d{2}:\\d{2}$", message = "时间格式为HH:mm")
        private String openTime;

        @NotBlank(message = "结束时间不能为空")
        @Pattern(regexp = "^\\d{2}:\\d{2}$", message = "时间格式为HH:mm")
        private String closeTime;
    }
}
