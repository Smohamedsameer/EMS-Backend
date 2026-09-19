package com.ems.service;

import com.ems.dto.AttendanceCorrectionRequest;
import com.ems.dto.AttendanceResponse;
import com.ems.dto.MonthlyAttendanceResponse;
import com.ems.entity.Attendance;
import com.ems.entity.AttendanceStatus;
import com.ems.entity.Employee;
import com.ems.entity.Holiday;
import com.ems.exception.BadRequestException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.repository.AttendanceRepository;
import com.ems.repository.EmployeeRepository;
import com.ems.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private static final LocalTime LATE_THRESHOLD = LocalTime.of(10, 0);
    private static final double HALF_DAY_HOUR_LIMIT = 4.0;

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final HolidayRepository holidayRepository;

    @Transactional
    public AttendanceResponse checkIn(Employee employee) {
        LocalDate today = LocalDate.now();
        Optional<Attendance> existing = attendanceRepository.findByEmployeeIdAndAttendanceDate(employee.getId(), today);

        if (existing.isPresent() && existing.get().getCheckIn() != null) {
            throw new BadRequestException("You have already checked in today");
        }

        LocalDateTime now = LocalDateTime.now();
        AttendanceStatus status = now.toLocalTime().isAfter(LATE_THRESHOLD) ? AttendanceStatus.LATE : AttendanceStatus.PRESENT;

        Attendance attendance = existing.orElseGet(() -> Attendance.builder()
                .employee(employee)
                .attendanceDate(today)
                .build());
        attendance.setCheckIn(now);
        attendance.setStatus(status);

        attendance = attendanceRepository.save(attendance);
        return toResponse(attendance);
    }

    @Transactional
    public AttendanceResponse checkOut(Employee employee) {
        LocalDate today = LocalDate.now();
        Attendance attendance = attendanceRepository.findByEmployeeIdAndAttendanceDate(employee.getId(), today)
                .orElseThrow(() -> new BadRequestException("You must check in before checking out"));

        if (attendance.getCheckIn() == null) {
            throw new BadRequestException("You must check in before checking out");
        }
        if (attendance.getCheckOut() != null) {
            throw new BadRequestException("You have already checked out today");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(attendance.getCheckIn())) {
            throw new BadRequestException("Checkout time cannot be before check-in time");
        }

        attendance.setCheckOut(now);
        double hours = Duration.between(attendance.getCheckIn(), now).toMinutes() / 60.0;
        hours = Math.round(hours * 100.0) / 100.0;
        attendance.setWorkingHours(hours);

        if (hours < HALF_DAY_HOUR_LIMIT) {
            attendance.setStatus(AttendanceStatus.HALF_DAY);
        }

        attendance = attendanceRepository.save(attendance);
        return toResponse(attendance);
    }

    public AttendanceResponse getTodayAttendance(Employee employee) {
        return attendanceRepository.findByEmployeeIdAndAttendanceDate(employee.getId(), LocalDate.now())
                .map(this::toResponse)
                .orElse(null);
    }

    public List<AttendanceResponse> getHistory(Long employeeId) {
        return attendanceRepository.findByEmployeeIdOrderByAttendanceDateDesc(employeeId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AttendanceResponse> search(Long employeeId, LocalDate date, String department, String status) {
        AttendanceStatus statusEnum = (status == null || status.isBlank()) ? null : AttendanceStatus.valueOf(status.toUpperCase());
        String dept = (department == null || department.isBlank()) ? null : department;
        return attendanceRepository.search(employeeId, date, dept, statusEnum).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AttendanceResponse correctAttendance(Long attendanceId, AttendanceCorrectionRequest request) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found"));

        if (request.getCheckIn() != null) {
            attendance.setCheckIn(request.getCheckIn());
        }
        if (request.getCheckOut() != null) {
            attendance.setCheckOut(request.getCheckOut());
        }
        if (attendance.getCheckIn() != null && attendance.getCheckOut() != null) {
            if (attendance.getCheckOut().isBefore(attendance.getCheckIn())) {
                throw new BadRequestException("Check-out time cannot be before check-in time");
            }
            double hours = Duration.between(attendance.getCheckIn(), attendance.getCheckOut()).toMinutes() / 60.0;
            attendance.setWorkingHours(Math.round(hours * 100.0) / 100.0);
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            attendance.setStatus(AttendanceStatus.valueOf(request.getStatus().toUpperCase()));
        }

        attendance = attendanceRepository.save(attendance);
        return toResponse(attendance);
    }

    /** Ensures a LEAVE attendance record exists for a given date, without overwriting real attendance. */
    @Transactional
    public void markLeaveForDate(Employee employee, LocalDate date) {
        Attendance attendance = attendanceRepository.findByEmployeeIdAndAttendanceDate(employee.getId(), date)
                .orElseGet(() -> Attendance.builder().employee(employee).attendanceDate(date).build());

        if (attendance.getCheckIn() == null) {
            attendance.setStatus(AttendanceStatus.LEAVE);
            attendanceRepository.save(attendance);
        }
    }

    public Map<String, Long> monthlyAttendanceCounts(YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : attendanceRepository.monthlyAttendanceCounts(start, end)) {
            result.put(row[0].toString(), (Long) row[1]);
        }
        return result;
    }

    public long countByDateAndStatus(LocalDate date, AttendanceStatus status) {
        return attendanceRepository.countByAttendanceDateAndStatus(date, status);
    }

    /**
     * Builds the admin's month-wise attendance grid: one row per employee, one column per day
     * of the given month, with a status code per cell and running totals.
     *
     * Rules:
     *  - A date with a declared company Holiday -> "H" for every employee, regardless of anything else.
     *  - A future date (after today) that isn't a holiday -> "-" (hasn't happened yet).
     *  - A date before the employee's joining date -> "-" (not employed yet).
     *  - Otherwise: an attendance record with status PRESENT/LATE -> "P", HALF_DAY -> "HD",
     *    LEAVE (approved personal leave) -> "L", ABSENT -> "A".
     *  - No attendance record at all for a past working date -> "A" (never checked in = absent).
     *
     * "Total working days" counts only cells that are P/HD/A/L for that employee (i.e. holidays and
     * not-yet-applicable days are excluded).
     */
    public MonthlyAttendanceResponse getMonthlyGrid(YearMonth month) {
        LocalDate today = LocalDate.now();
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        int daysInMonth = end.getDayOfMonth();

        Map<LocalDate, String> holidayMap = new HashMap<>();
        for (Holiday h : holidayRepository.findByHolidayDateBetweenOrderByHolidayDateAsc(start, end)) {
            holidayMap.put(h.getHolidayDate(), h.getReason());
        }

        Map<Long, Map<LocalDate, Attendance>> byEmployeeDate = new HashMap<>();
        for (Attendance a : attendanceRepository.findByAttendanceDateBetween(start, end)) {
            byEmployeeDate
                    .computeIfAbsent(a.getEmployee().getId(), k -> new HashMap<>())
                    .put(a.getAttendanceDate(), a);
        }

        List<MonthlyAttendanceResponse.DayMeta> days = new ArrayList<>();
        for (int d = 1; d <= daysInMonth; d++) {
            LocalDate date = month.atDay(d);
            days.add(MonthlyAttendanceResponse.DayMeta.builder()
                    .day(d)
                    .date(date)
                    .weekday(date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase())
                    .sunday(date.getDayOfWeek() == DayOfWeek.SUNDAY)
                    .holiday(holidayMap.containsKey(date))
                    .holidayReason(holidayMap.get(date))
                    .future(date.isAfter(today))
                    .build());
        }

        List<Employee> employees = employeeRepository.findAll(
                org.springframework.data.domain.Sort.by("name"));

        List<MonthlyAttendanceResponse.EmployeeMonthRow> rows = new ArrayList<>();
        for (Employee employee : employees) {
            Map<LocalDate, Attendance> records = byEmployeeDate.getOrDefault(employee.getId(), Map.of());
            List<String> codes = new ArrayList<>(daysInMonth);

            int workingDays = 0, present = 0, absent = 0, leave = 0, holidays = 0, halfDays = 0;

            for (int d = 1; d <= daysInMonth; d++) {
                LocalDate date = month.atDay(d);
                String code;

                if (holidayMap.containsKey(date)) {
                    code = "H";
                    holidays++;
                } else if (date.isAfter(today)) {
                    code = "-";
                } else if (employee.getJoiningDate() != null && date.isBefore(employee.getJoiningDate())) {
                    code = "-";
                } else {
                    Attendance a = records.get(date);
                    if (a == null) {
                        code = "A";
                        absent++;
                    } else {
                        switch (a.getStatus()) {
                            case PRESENT, LATE -> {
                                code = "P";
                                present++;
                            }
                            case HALF_DAY -> {
                                code = "HD";
                                halfDays++;
                            }
                            case LEAVE -> {
                                code = "L";
                                leave++;
                            }
                            default -> {
                                code = "A";
                                absent++;
                            }
                        }
                    }
                    workingDays++;
                }

                codes.add(code);
            }

            rows.add(MonthlyAttendanceResponse.EmployeeMonthRow.builder()
                    .employeeDbId(employee.getId())
                    .employeeId(employee.getEmployeeId())
                    .employeeName(employee.getName())
                    .department(employee.getDepartment())
                    .dayCodes(codes)
                    .totalWorkingDays(workingDays)
                    .totalPresent(present)
                    .totalAbsent(absent)
                    .totalLeave(leave)
                    .totalHolidays(holidays)
                    .totalHalfDays(halfDays)
                    .build());
        }

        return MonthlyAttendanceResponse.builder()
                .month(month.toString())
                .days(days)
                .employees(rows)
                .build();
    }

    private AttendanceResponse toResponse(Attendance a) {
        return AttendanceResponse.builder()
                .id(a.getId())
                .employeeId(a.getEmployee().getEmployeeId())
                .employeeName(a.getEmployee().getName())
                .department(a.getEmployee().getDepartment())
                .attendanceDate(a.getAttendanceDate())
                .checkIn(a.getCheckIn())
                .checkOut(a.getCheckOut())
                .workingHours(a.getWorkingHours())
                .status(a.getStatus().name())
                .build();
    }
}
