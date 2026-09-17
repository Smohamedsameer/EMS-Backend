package com.ems.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardEmployeeResponse {
    private String employeeName;
    private AttendanceResponse todayAttendance;

    private long assignedCount;
    private long inProgressCount;
    private long completedCount;
    private long pendingCount;

    private List<TaskResponse> recentTasks;
    private List<NotificationResponse> recentNotifications;
}
