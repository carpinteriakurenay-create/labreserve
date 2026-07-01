package com.labreserve.controller;

import com.labreserve.dto.ApiResponse;
import com.labreserve.dto.DashboardKpiVO;
import com.labreserve.dto.EquipmentUsageStatVO;
import com.labreserve.dto.LabUsageStatVO;
import com.labreserve.dto.StudentRankingVO;
import com.labreserve.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/kpi")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<DashboardKpiVO> getKpi() {
        Long userId = getCurrentUserId();
        String role = getCurrentUserRole();
        return ApiResponse.success(dashboardService.getKpi(userId, role));
    }

    @GetMapping("/lab-usage")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<LabUsageStatVO>> getLabUsage(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        Long userId = getCurrentUserId();
        String role = getCurrentUserRole();
        return ApiResponse.success(dashboardService.getLabUsage(userId, role, dateFrom, dateTo));
    }

    @GetMapping("/equipment-usage")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<EquipmentUsageStatVO>> getEquipmentUsage(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        Long userId = getCurrentUserId();
        String role = getCurrentUserRole();
        return ApiResponse.success(dashboardService.getEquipmentUsage(userId, role, dateFrom, dateTo));
    }

    @GetMapping("/student-ranking")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<StudentRankingVO>> getStudentRanking(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(dashboardService.getStudentRanking(dateFrom, dateTo, limit));
    }

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }

    private String getCurrentUserRole() {
        return SecurityContextHolder.getContext()
                .getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .orElse("STUDENT");
    }
}
