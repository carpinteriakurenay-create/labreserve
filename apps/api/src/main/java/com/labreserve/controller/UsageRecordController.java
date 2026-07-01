package com.labreserve.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.labreserve.dto.ApiResponse;
import com.labreserve.dto.UsageRecordVO;
import com.labreserve.service.UsageRecordService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/usage-records")
@PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
public class UsageRecordController {

    private final UsageRecordService usageRecordService;

    public UsageRecordController(UsageRecordService usageRecordService) {
        this.usageRecordService = usageRecordService;
    }

    @GetMapping
    public ApiResponse<IPage<UsageRecordVO>> list(
            @RequestParam(required = false) Long labId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        IPage<UsageRecordVO> page = usageRecordService.listUsageRecords(
                labId, userId, dateFrom, dateTo, pageNum, pageSize);
        return ApiResponse.success(page);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) Long labId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        String csv = usageRecordService.exportCsv(labId, userId, dateFrom, dateTo);
        byte[] csvBytes = csv.getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("usage-records.csv", StandardCharsets.UTF_8)
                .build());

        return ResponseEntity.ok().headers(headers).body(csvBytes);
    }
}
