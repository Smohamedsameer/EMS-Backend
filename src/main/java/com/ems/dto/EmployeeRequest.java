package com.ems.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class EmployeeRequest {

    @NotBlank(message = "Employee ID is required")
    private String employeeId;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    private String phone;

    /** Required on create, optional on update (leave blank to keep existing password) */
    private String password;

    private String department;

    private String designation;

    private LocalDate joiningDate;

    /** ACTIVE or INACTIVE, defaults to ACTIVE on create */
    private String status;
}
