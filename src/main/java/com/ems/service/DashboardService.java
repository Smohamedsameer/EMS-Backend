package com.ems.service;

import com.ems.dto.*;
import com.ems.entity.*;
import com.ems.repository.EmployeeRepository;
import com.ems.repository.LeaveRequestRepository;
import com.ems.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final EmployeeRepository employeeRepository;
    private final TaskRepository taskRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AttendanceService attendanceService;
    private final TaskService taskService;
    private final NotificationService notificationService;

    public DashboardAdminResponse getAdminDashboard() {
        LocalDate today = LocalDate.now();

        long totalEmployees = employeeRepository.countByStatus(EmployeeStatus.ACTIVE);
        long present = attendanceService.countByDateAndStatus(today, AttendanceStatus.PRESENT)
                + attendanceService.countByDateAndStatus(today, AttendanceStatus.LATE);
        long onLeave = attendanceService.countByDateAndStatus(today, AttendanceStatus.LEAVE);
        long absent = Math.max(totalEmployees - present - onLeave, 0);

        long totalTasks = taskRepository.count();
        long inProgress = taskRepository.countByStatus(TaskStatus.IN_PROGRESS);
        long completed = taskRepository.countByStatus(TaskStatus.COMPLETED);
        long pending = taskRepository.countByStatus(TaskStatus.ASSIGNED);

        Map<String, Long> taskBreakdown = new LinkedHashMap<>();
        for (TaskStatus status : TaskStatus.values()) {
            taskBreakdown.put(status.name(), taskRepository.countByStatus(status));
        }

        Map<String, Long> monthlyAttendance = attendanceService.monthlyAttendanceCounts(YearMonth.from(today));

        List<DashboardAdminResponse.EmployeeWorkload> workload = employeeRepository
                .search(null, null, EmployeeStatus.ACTIVE).stream()
                .map(e -> DashboardAdminResponse.EmployeeWorkload.builder()
                        .employeeName(e.getName())
                        .taskCount(taskRepository.countByAssignedToId(e.getId()) -
                                taskRepository.countByAssignedToIdAndStatus(e.getId(), TaskStatus.COMPLETED))
                        .build())
                .toList();

        return DashboardAdminResponse.builder()
                .totalEmployees(totalEmployees)
                .presentToday(present)
                .absentToday(absent)
                .onLeaveToday(onLeave)
                .totalAssignedTasks(totalTasks)
                .inProgressTasks(inProgress)
                .completedTasks(completed)
                .pendingTasks(pending)
                .monthlyAttendance(monthlyAttendance)
                .taskStatusBreakdown(taskBreakdown)
                .employeeWorkload(workload)
                .build();
    }

    public DashboardEmployeeResponse getEmployeeDashboard(Employee employee, Long userId) {
        AttendanceResponse today = attendanceService.getTodayAttendance(employee);

        long assigned = taskRepository.countByAssignedToId(employee.getId());
        long inProgress = taskRepository.countByAssignedToIdAndStatus(employee.getId(), TaskStatus.IN_PROGRESS);
        long completed = taskRepository.countByAssignedToIdAndStatus(employee.getId(), TaskStatus.COMPLETED);
        long pending = taskRepository.countByAssignedToIdAndStatus(employee.getId(), TaskStatus.ASSIGNED);

        List<TaskResponse> recentTasks = taskService.getMyTasks(employee.getId()).stream().limit(5).toList();
        List<NotificationResponse> notifications = notificationService.getMyNotifications(userId).stream().limit(10).toList();

        return DashboardEmployeeResponse.builder()
                .employeeName(employee.getName())
                .todayAttendance(today)
                .assignedCount(assigned)
                .inProgressCount(inProgress)
                .completedCount(completed)
                .pendingCount(pending)
                .recentTasks(recentTasks)
                .recentNotifications(notifications)
                .build();
    }
}
