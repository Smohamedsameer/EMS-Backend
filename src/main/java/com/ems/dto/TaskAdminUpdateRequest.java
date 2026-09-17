package com.ems.dto;

import lombok.Data;

import java.time.LocalDate;

/** Used by admin to reassign, change priority/deadline, or approve a task */
@Data
public class TaskAdminUpdateRequest {
    private String title;
    private String description;
    private String project;
    private Long assignedToId;
    private String priority;
    private LocalDate startDate;
    private LocalDate dueDate;
    private String status;
    private String adminComment;
    private Boolean approved;
}
