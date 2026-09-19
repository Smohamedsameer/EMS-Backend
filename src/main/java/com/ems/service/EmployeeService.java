package com.ems.service;

import com.ems.dto.EmployeeDetailResponse;
import com.ems.dto.EmployeeRequest;
import com.ems.dto.EmployeeResponse;
import com.ems.entity.*;
import com.ems.exception.BadRequestException;
import com.ems.exception.DuplicateResourceException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.repository.AttendanceRepository;
import com.ems.repository.EmployeeRepository;
import com.ems.repository.TaskRepository;
import com.ems.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final TaskRepository taskRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        if (employeeRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new DuplicateResourceException("Employee ID already exists: " + request.getEmployeeId());
        }
        if (employeeRepository.existsByEmail(request.getEmail()) || userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already in use: " + request.getEmail());
        }
        if (userRepository.existsByUsername(request.getEmployeeId())) {
            throw new DuplicateResourceException("A user account already exists for this Employee ID");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BadRequestException("Password is required to create an employee login");
        }

        User user = User.builder()
                .username(request.getEmployeeId())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.EMPLOYEE)
                .enabled(true)
                .build();
        user = userRepository.save(user);

        EmployeeStatus status = parseStatus(request.getStatus(), EmployeeStatus.ACTIVE);

        Employee employee = Employee.builder()
                .employeeId(request.getEmployeeId())
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .department(request.getDepartment())
                .designation(request.getDesignation())
                .joiningDate(request.getJoiningDate())
                .status(status)
                .user(user)
                .build();
        employee = employeeRepository.save(employee);

        return toResponse(employee);
    }
    @Transactional(readOnly = true)
    public List<EmployeeResponse> getAllEmployees(String search, String department, String status) {
        EmployeeStatus statusEnum = (status == null || status.isBlank()) ? null : EmployeeStatus.valueOf(status.toUpperCase());
        String s = (search == null || search.isBlank()) ? null : search;
        String d = (department == null || department.isBlank()) ? null : department;
        return employeeRepository.search(s, d, statusEnum).stream().map(this::toResponse).toList();
    }
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {
        return toResponse(getEmployeeEntity(id));
    }
    @Transactional(readOnly = true)
    public Employee getEmployeeEntity(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
    }
    @Transactional(readOnly = true)
    public Employee getEmployeeByUserId(Long userId) {
        return employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No employee profile linked to this account"));
    }
    @Transactional(readOnly = true)
    public EmployeeDetailResponse getEmployeeDetail(Long id) {
        Employee employee = getEmployeeEntity(id);

        long present = attendanceRepository.countByEmployeeAndStatus(id, AttendanceStatus.PRESENT);
        long absent = attendanceRepository.countByEmployeeAndStatus(id, AttendanceStatus.ABSENT);
        long leave = attendanceRepository.countByEmployeeAndStatus(id, AttendanceStatus.LEAVE);
        long late = attendanceRepository.countByEmployeeAndStatus(id, AttendanceStatus.LATE);

        long assigned = taskRepository.countByAssignedToId(id);
        long completed = taskRepository.countByAssignedToIdAndStatus(id, TaskStatus.COMPLETED);
        long inProgress = taskRepository.countByAssignedToIdAndStatus(id, TaskStatus.IN_PROGRESS);
        long pending = taskRepository.countByAssignedToIdAndStatus(id, TaskStatus.ASSIGNED);

        return EmployeeDetailResponse.builder()
                .profile(toResponse(employee))
                .presentCount(present)
                .absentCount(absent)
                .leaveCount(leave)
                .lateCount(late)
                .assignedTasks(assigned)
                .completedTasks(completed)
                .pendingTasks(pending)
                .inProgressTasks(inProgress)
                .build();
    }

    @Transactional(readOnly = true)
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        Employee employee = getEmployeeEntity(id);

        if (!employee.getEmployeeId().equals(request.getEmployeeId())
                && employeeRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new DuplicateResourceException("Employee ID already exists: " + request.getEmployeeId());
        }
        if (!employee.getEmail().equalsIgnoreCase(request.getEmail())
                && (employeeRepository.existsByEmail(request.getEmail()) || userRepository.existsByEmail(request.getEmail()))) {
            throw new DuplicateResourceException("Email already in use: " + request.getEmail());
        }

        employee.setEmployeeId(request.getEmployeeId());
        employee.setName(request.getName());
        employee.setEmail(request.getEmail());
        employee.setPhone(request.getPhone());
        employee.setDepartment(request.getDepartment());
        employee.setDesignation(request.getDesignation());
        employee.setJoiningDate(request.getJoiningDate());
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            employee.setStatus(parseStatus(request.getStatus(), employee.getStatus()));
        }

        User user = employee.getUser();
        if (user != null) {
            user.setEmail(request.getEmail());
            user.setUsername(request.getEmployeeId());
            if (request.getPassword() != null && !request.getPassword().isBlank()) {
                user.setPassword(passwordEncoder.encode(request.getPassword()));
            }
            user.setEnabled(employee.getStatus() == EmployeeStatus.ACTIVE);
            userRepository.save(user);
        }

        employee = employeeRepository.save(employee);
        return toResponse(employee);
    }

    @Transactional(readOnly = true)
    public void deactivateEmployee(Long id) {
        Employee employee = getEmployeeEntity(id);
        employee.setStatus(EmployeeStatus.INACTIVE);
        employeeRepository.save(employee);

        if (employee.getUser() != null) {
            employee.getUser().setEnabled(false);
            userRepository.save(employee.getUser());
        }
    }

    private EmployeeStatus parseStatus(String status, EmployeeStatus fallback) {
        if (status == null || status.isBlank()) {
            return fallback;
        }
        try {
            return EmployeeStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + status);
        }
    }

    private EmployeeResponse toResponse(Employee e) {
        return EmployeeResponse.builder()
                .id(e.getId())
                .employeeId(e.getEmployeeId())
                .name(e.getName())
                .email(e.getEmail())
                .phone(e.getPhone())
                .department(e.getDepartment())
                .designation(e.getDesignation())
                .joiningDate(e.getJoiningDate())
                .status(e.getStatus().name())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
