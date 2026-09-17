package com.ems.repository;

import com.ems.entity.Employee;
import com.ems.entity.EmployeeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmployeeId(String employeeId);

    Optional<Employee> findByEmail(String email);

    Optional<Employee> findByUserId(Long userId);

    boolean existsByEmployeeId(String employeeId);

    boolean existsByEmail(String email);

    long countByStatus(EmployeeStatus status);

    @Query("SELECT e FROM Employee e WHERE " +
            "(:search IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(e.employeeId) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(e.email) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:department IS NULL OR e.department = :department) " +
            "AND (:status IS NULL OR e.status = :status)")
    List<Employee> search(@Param("search") String search,
                           @Param("department") String department,
                           @Param("status") EmployeeStatus status);
}
