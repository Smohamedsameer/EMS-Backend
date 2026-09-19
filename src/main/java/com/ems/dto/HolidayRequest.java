package com.ems.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class HolidayRequest {

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotBlank(message = "Reason is required")
    private String reason;
}
