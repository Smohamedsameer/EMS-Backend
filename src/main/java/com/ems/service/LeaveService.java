package com.ems.service;

import com.ems.dto.LeaveRequestDto;
import com.ems.dto.LeaveResponse;
import com.ems.entity.Employee;
import com.ems.entity.LeaveRequest;
import com.ems.entity.LeaveStatus;
import com.ems.entity.NotificationType;
import com.ems.exception.BadRequestException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final NotificationService notificationService;
    private final AttendanceService attendanceService;

    @Transactional
    public LeaveResponse applyLeave(Employee employee, LeaveRequestDto request) {
        if (request.getToDate().isBefore(request.getFromDate())) {
            throw new BadRequestException("To date cannot be before from date");
        }

        LeaveRequest leave = LeaveRequest.builder()
                .employee(employee)
                .leaveType(request.getLeaveType())
                .fromDate(request.getFromDate())
                .toDate(request.getToDate())
                .reason(request.getReason())
                .status(LeaveStatus.PENDING)
                .build();
        leave = leaveRequestRepository.save(leave);

        notificationService.notifyAllAdmins(
                employee.getName() + " requested " + request.getLeaveType() + " leave", NotificationType.LEAVE_REQUESTED);

        return toResponse(leave);
    }

    @Transactional(readOnly = true)
    public List<LeaveResponse> getMyLeaves(Long employeeId) {
        return leaveRequestRepository
                .findByEmployeeIdOrderByCreatedAtDesc(employeeId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveResponse> getAllLeaves() {
        return leaveRequestRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }
    @Transactional
    public LeaveResponse approveLeave(Long id, String adminComment) {
        LeaveRequest leave = getEntity(id);
        leave.setStatus(LeaveStatus.APPROVED);
        leave.setAdminComment(adminComment);
        leave = leaveRequestRepository.save(leave);

        LocalDate date = leave.getFromDate();
        while (!date.isAfter(leave.getToDate())) {
            attendanceService.markLeaveForDate(leave.getEmployee(), date);
            date = date.plusDays(1);
        }

        if (leave.getEmployee().getUser() != null) {
            notificationService.notifyUser(leave.getEmployee().getUser().getId(),
                    "Your " + leave.getLeaveType() + " leave request was approved", NotificationType.LEAVE_APPROVED);
        }

        return toResponse(leave);
    }

    @Transactional
    public LeaveResponse rejectLeave(Long id, String adminComment) {
        LeaveRequest leave = getEntity(id);
        leave.setStatus(LeaveStatus.REJECTED);
        leave.setAdminComment(adminComment);
        leave = leaveRequestRepository.save(leave);

        if (leave.getEmployee().getUser() != null) {
            notificationService.notifyUser(leave.getEmployee().getUser().getId(),
                    "Your " + leave.getLeaveType() + " leave request was rejected", NotificationType.LEAVE_REJECTED);
        }

        return toResponse(leave);
    }

    private LeaveRequest getEntity(Long id) {
        return leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with id: " + id));
    }

    private LeaveResponse toResponse(LeaveRequest l) {
        return LeaveResponse.builder()
                .id(l.getId())
                .employeeId(l.getEmployee().getEmployeeId())
                .employeeName(l.getEmployee().getName())
                .leaveType(l.getLeaveType())
                .fromDate(l.getFromDate())
                .toDate(l.getToDate())
                .reason(l.getReason())
                .status(l.getStatus().name())
                .adminComment(l.getAdminComment())
                .createdAt(l.getCreatedAt())
                .build();
    }
}
