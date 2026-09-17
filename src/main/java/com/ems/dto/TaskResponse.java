package com.ems.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskResponse {
    private Long id;
    private String taskId;
    private String title;
    private String description;
    private String project;
    private Long assignedToId;
    private String assignedToEmployeeCode;
    private String assignedToName;
    private String assignedByName;
    private String priority;
    private LocalDate startDate;
    private LocalDate dueDate;
    private String status;
    private Integer progress;
    private String adminComment;
    private boolean approved;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
