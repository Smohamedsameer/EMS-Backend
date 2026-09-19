package com.ems.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Payload for the admin's month-wise attendance grid:
 * one column per day of the selected month, one row per employee,
 * each cell holding a status code (P / A / L / H / HD / "-").
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyAttendanceResponse {

    /** e.g. "2026-09" */
    private String month;

    /** Meta info for every day column in the month. */
    private List<DayMeta> days;

    /** One row per employee. */
    private List<EmployeeMonthRow> employees;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DayMeta {
        private int day;
        private LocalDate date;
        /** MON, TUE, ... */
        private String weekday;
        private boolean sunday;
        private boolean holiday;
        private String holidayReason;
        /** true if this date is after today, i.e. hasn't happened yet */
        private boolean future;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EmployeeMonthRow {
        private Long employeeDbId;
        private String employeeId;
        private String employeeName;
        private String department;

        /**
         * One status code per day of the month, in order (index 0 = day 1):
         * "P" present/late, "HD" half day, "A" absent, "L" approved leave,
         * "H" company holiday, "-" not applicable (before joining or day hasn't occurred yet).
         */
        private List<String> dayCodes;

        private int totalWorkingDays;
        private int totalPresent;
        private int totalAbsent;
        private int totalLeave;
        private int totalHolidays;
        private int totalHalfDays;
    }
}
