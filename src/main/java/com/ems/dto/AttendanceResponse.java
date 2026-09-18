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
public class AttendanceResponse {
    private Long id;
    private String employeeId;
    private String employeeName;
    private String department;
    private LocalDate attendanceDate;
    private LocalDateTime checkIn;
    private Double checkInLatitude;
    private Double checkInLongitude;
    private LocalDateTime checkOut;
    private Double checkOutLatitude;
    private Double checkOutLongitude;
    private Double workingHours;
    private String status;
}