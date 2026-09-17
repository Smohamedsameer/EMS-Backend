package com.ems.controller;

import com.ems.dto.LeaveActionRequest;
import com.ems.dto.LeaveRequestDto;
import com.ems.dto.LeaveResponse;
import com.ems.entity.Employee;
import com.ems.security.CurrentUser;
import com.ems.service.EmployeeService;
import com.ems.service.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;
    private final EmployeeService employeeService;

    @PostMapping
    public ResponseEntity<LeaveResponse> apply(@Valid @RequestBody LeaveRequestDto request) {
        Employee employee = employeeService.getEmployeeByUserId(CurrentUser.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(leaveService.applyLeave(employee, request));
    }

    @GetMapping("/my-leaves")
    public ResponseEntity<List<LeaveResponse>> myLeaves() {
        Employee employee = employeeService.getEmployeeByUserId(CurrentUser.userId());
        return ResponseEntity.ok(leaveService.getMyLeaves(employee.getId()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LeaveResponse>> all() {
        return ResponseEntity.ok(leaveService.getAllLeaves());
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LeaveResponse> approve(@PathVariable Long id, @RequestBody(required = false) LeaveActionRequest request) {
        String comment = request == null ? null : request.getAdminComment();
        return ResponseEntity.ok(leaveService.approveLeave(id, comment));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LeaveResponse> reject(@PathVariable Long id, @RequestBody(required = false) LeaveActionRequest request) {
        String comment = request == null ? null : request.getAdminComment();
        return ResponseEntity.ok(leaveService.rejectLeave(id, comment));
    }
}
