package com.ems.repository;

import com.ems.entity.LeaveRequest;
import com.ems.entity.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    List<LeaveRequest> findAllByOrderByCreatedAtDesc();

    long countByStatus(LeaveStatus status);

    @Query("SELECT COUNT(l) > 0 FROM LeaveRequest l WHERE l.employee.id = :employeeId " +
            "AND l.status = 'APPROVED' AND :date BETWEEN l.fromDate AND l.toDate")
    boolean existsApprovedLeaveForDate(@Param("employeeId") Long employeeId, @Param("date") LocalDate date);

    @Query("SELECT l FROM LeaveRequest l WHERE l.status = 'APPROVED' AND l.fromDate <= :date AND l.toDate >= :date")
    List<LeaveRequest> findApprovedLeavesForDate(@Param("date") LocalDate date);
}
