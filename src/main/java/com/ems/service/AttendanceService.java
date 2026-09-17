package com.ems.service;

import com.ems.dto.AttendanceCorrectionRequest;
import com.ems.dto.AttendanceResponse;
import com.ems.entity.Attendance;
import com.ems.entity.AttendanceStatus;
import com.ems.entity.Employee;
import com.ems.exception.BadRequestException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private static final LocalTime LATE_THRESHOLD = LocalTime.of(10, 0);
    private static final double HALF_DAY_HOUR_LIMIT = 4.0;

    private final AttendanceRepository attendanceRepository;

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

    @Transactional(readOnly = true)
    public AttendanceResponse getTodayAttendance(Employee employee) {
        return attendanceRepository
                .findByEmployeeIdAndAttendanceDate(employee.getId(), LocalDate.now())
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getHistory(Long employeeId) {
        return attendanceRepository
                .findByEmployeeIdOrderByAttendanceDateDesc(employeeId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> search(
            Long employeeId,
            LocalDate date,
            String department,
            String status) {

        AttendanceStatus statusEnum =
                (status == null || status.isBlank())
                        ? null
                        : AttendanceStatus.valueOf(status.toUpperCase());

        String dept =
                (department == null || department.isBlank())
                        ? null
                        : department;

        return attendanceRepository
                .search(employeeId, date, dept, statusEnum)
                .stream()
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
