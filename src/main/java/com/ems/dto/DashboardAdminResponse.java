package com.ems.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardAdminResponse {
    private long totalEmployees;
    private long presentToday;
    private long absentToday;
    private long onLeaveToday;
    private long totalAssignedTasks;
    private long inProgressTasks;
    private long completedTasks;
    private long pendingTasks;

    /** date -> present count, for the current month */
    private Map<String, Long> monthlyAttendance;

    /** status -> count */
    private Map<String, Long> taskStatusBreakdown;

    /** employee name -> active (non completed) task count */
    private List<EmployeeWorkload> employeeWorkload;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EmployeeWorkload {
        private String employeeName;
        private long taskCount;
    }
}
