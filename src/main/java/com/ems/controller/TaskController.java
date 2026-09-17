package com.ems.controller;

import com.ems.dto.*;
import com.ems.entity.Employee;
import com.ems.security.CurrentUser;
import com.ems.service.EmployeeService;
import com.ems.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final EmployeeService employeeService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TaskResponse>> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(taskService.getAllTasks(status, priority, employeeId, search));
    }

    @GetMapping("/my-tasks")
    public ResponseEntity<List<TaskResponse>> myTasks() {
        Employee employee = employeeService.getEmployeeByUserId(CurrentUser.userId());
        return ResponseEntity.ok(taskService.getMyTasks(employee.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    @GetMapping("/{id}/updates")
    public ResponseEntity<List<TaskUpdateResponse>> history(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskHistory(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(request, CurrentUser.userId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TaskResponse> update(@PathVariable Long id, @RequestBody TaskAdminUpdateRequest request) {
        return ResponseEntity.ok(taskService.updateTaskAdmin(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<TaskResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody TaskStatusUpdateRequest request) {
        Employee employee = employeeService.getEmployeeByUserId(CurrentUser.userId());
        return ResponseEntity.ok(taskService.updateStatusByEmployee(id, employee.getId(), request));
    }

    @PostMapping("/{id}/updates")
    public ResponseEntity<TaskUpdateResponse> addUpdate(@PathVariable Long id, @RequestBody TaskUpdateRequest request) {
        Employee employee = employeeService.getEmployeeByUserId(CurrentUser.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.addTaskUpdate(id, employee.getId(), request));
    }
}
