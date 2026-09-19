package com.ems.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A company-wide holiday declared by the admin for a specific date (e.g. Diwali, Independence Day).
 * Unlike {@link LeaveRequest} (which applies to one employee), a Holiday applies to every employee
 * and is shown as "H" in the monthly attendance grid, overriding whatever else happened that day.
 */
@Entity
@Table(name = "holidays", uniqueConstraints = {
        @UniqueConstraint(columnNames = "holiday_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "holiday_date", nullable = false, unique = true)
    private LocalDate holidayDate;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
