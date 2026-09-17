package com.ems.controller;

import com.ems.dto.DashboardAdminResponse;
import com.ems.dto.DashboardEmployeeResponse;
import com.ems.entity.Employee;
import com.ems.security.CurrentUser;
import com.ems.service.DashboardService;
import com.ems.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final EmployeeService employeeService;

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DashboardAdminResponse> admin() {
        return ResponseEntity.ok(dashboardService.getAdminDashboard());
    }

    @GetMapping("/employee")
    public ResponseEntity<DashboardEmployeeResponse> employee() {
        Long userId = CurrentUser.userId();
        Employee employee = employeeService.getEmployeeByUserId(userId);
        return ResponseEntity.ok(dashboardService.getEmployeeDashboard(employee, userId));
    }
}
