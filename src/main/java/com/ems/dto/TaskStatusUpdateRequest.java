package com.ems.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TaskStatusUpdateRequest {

    @NotBlank(message = "Status is required")
    private String status;

    private Integer progress;

    private String comment;

    private String attachmentUrl;
}
