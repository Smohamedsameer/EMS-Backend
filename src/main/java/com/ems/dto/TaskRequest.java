package com.ems.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private String project;

    @NotNull(message = "Assigned employee is required")
    private Long assignedToId;

    @NotBlank(message = "Priority is required")
    private String priority;

    private LocalDate startDate;

    private LocalDate dueDate;
}
