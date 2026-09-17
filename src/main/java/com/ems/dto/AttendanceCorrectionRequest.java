 package com.ems.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AttendanceCorrectionRequest {
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    private Double latitude;
    private Double longitude;
    private String status;
}
