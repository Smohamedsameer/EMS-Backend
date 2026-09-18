package com.ems.controller;

import com.ems.dto.AttendanceCorrectionRequest;
import com.ems.dto.AttendanceResponse;
import com.ems.entity.Employee;
import com.ems.security.CurrentUser;
import com.ems.service.AttendanceService;
import com.ems.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

	private final EmployeeService employeeService;
    private final AttendanceService attendanceService;

    @PostMapping("/check-in")
    public ResponseEntity<AttendanceResponse> checkIn() {
        Employee employee = employeeService.getEmployeeByUserId(CurrentUser.userId());
        return ResponseEntity.ok(attendanceService.checkIn(employee));
    }

    @PostMapping("/check-out")
    public ResponseEntity<AttendanceResponse> checkOut() {
        Employee employee = employeeService.getEmployeeByUserId(CurrentUser.userId());
        return ResponseEntity.ok(attendanceService.checkOut(employee));
    }

    @GetMapping("/today")
    public ResponseEntity<AttendanceResponse> today() {
        Employee employee = employeeService.getEmployeeByUserId(CurrentUser.userId());
        return ResponseEntity.ok(attendanceService.getTodayAttendance(employee));
    }

    @GetMapping("/my-history")
    public ResponseEntity<List<AttendanceResponse>> myHistory() {
        Employee employee = employeeService.getEmployeeByUserId(CurrentUser.userId());
        return ResponseEntity.ok(attendanceService.getHistory(employee.getId()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AttendanceResponse>> search(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(attendanceService.search(employeeId, date, department, status));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AttendanceResponse>> byEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(attendanceService.getHistory(employeeId));
    }

    @PutMapping("/{id}/correct")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AttendanceResponse> correct(@PathVariable Long id, @RequestBody AttendanceCorrectionRequest request) {
        return ResponseEntity.ok(attendanceService.correctAttendance(id, request));
    }
}
