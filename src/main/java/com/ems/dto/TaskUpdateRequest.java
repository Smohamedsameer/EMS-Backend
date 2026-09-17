package com.ems.dto;

import lombok.Data;

@Data
public class TaskUpdateRequest {
    private String status;
    private Integer progress;
    private String comment;
    private String attachmentUrl;
}
