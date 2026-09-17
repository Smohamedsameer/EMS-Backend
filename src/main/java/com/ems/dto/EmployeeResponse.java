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
public class EmployeeResponse {
    private Long id;
    private String employeeId;
    private String name;
    private String email;
    private String phone;
    private String department;
    private String designation;
    private LocalDate joiningDate;
    private String status;
    private LocalDateTime createdAt;
}
