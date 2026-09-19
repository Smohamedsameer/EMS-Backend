package com.ems.repository;

import com.ems.entity.Attendance;
import com.ems.entity.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate date);

    List<Attendance> findByEmployeeIdOrderByAttendanceDateDesc(Long employeeId);

    List<Attendance> findByAttendanceDate(LocalDate date);

    List<Attendance> findByAttendanceDateBetween(LocalDate start, LocalDate end);

    long countByAttendanceDateAndStatus(LocalDate date, AttendanceStatus status);

    @Query("SELECT a FROM Attendance a WHERE " +
            "(:employeeId IS NULL OR a.employee.id = :employeeId) " +
            "AND (:date IS NULL OR a.attendanceDate = :date) " +
            "AND (:department IS NULL OR a.employee.department = :department) " +
            "AND (:status IS NULL OR a.status = :status) " +
            "ORDER BY a.attendanceDate DESC")
    List<Attendance> search(@Param("employeeId") Long employeeId,
                             @Param("date") LocalDate date,
                             @Param("department") String department,
                             @Param("status") AttendanceStatus status);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.employee.id = :employeeId AND a.status = :status")
    long countByEmployeeAndStatus(@Param("employeeId") Long employeeId, @Param("status") AttendanceStatus status);

    @Query("SELECT a.attendanceDate, COUNT(a) FROM Attendance a WHERE a.status = 'PRESENT' " +
            "AND a.attendanceDate BETWEEN :start AND :end GROUP BY a.attendanceDate ORDER BY a.attendanceDate")
    List<Object[]> monthlyAttendanceCounts(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
