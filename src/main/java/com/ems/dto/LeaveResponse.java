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
public class LeaveResponse {
    private Long id;
    private String employeeId;
    private String employeeName;
    private String leaveType;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String reason;
    private String status;
    private String adminComment;
    private LocalDateTime createdAt;
}
