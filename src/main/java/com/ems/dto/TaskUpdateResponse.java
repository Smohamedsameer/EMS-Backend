package com.ems.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskUpdateResponse {
    private Long id;
    private Long taskId;
    private String taskTitle;
    private String employeeName;
    private String status;
    private Integer progress;
    private String comment;
    private String attachmentUrl;
    private LocalDateTime createdAt;
}
