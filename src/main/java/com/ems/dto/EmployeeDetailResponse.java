package com.ems.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDetailResponse {
    private EmployeeResponse profile;

    private long presentCount;
    private long absentCount;
    private long leaveCount;
    private long lateCount;

    private long assignedTasks;
    private long completedTasks;
    private long pendingTasks;
    private long inProgressTasks;
}
