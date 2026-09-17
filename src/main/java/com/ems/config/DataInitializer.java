package com.ems.config;

import com.ems.entity.*;
import com.ems.repository.EmployeeRepository;
import com.ems.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Seeds a demo ADMIN account and a demo EMPLOYEE account on first startup so the
 * application can be exercised immediately. Controlled by app.seed.enabled.
 *
 * IMPORTANT: these are local development credentials only. Change or remove
 * this seeding logic (or the app.seed.enabled flag) before any real deployment.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }

        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@ems.com")
                    .password(passwordEncoder.encode("Admin@123"))
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build();
            userRepository.save(admin);
            log.info("Seeded demo ADMIN user -> username: admin | password: Admin@123");
        }

        if (userRepository.findByUsername("EMP001").isEmpty()) {
            User empUser = User.builder()
                    .username("EMP001")
                    .email("employee@ems.com")
                    .password(passwordEncoder.encode("Employee@123"))
                    .role(Role.EMPLOYEE)
                    .enabled(true)
                    .build();
            empUser = userRepository.save(empUser);

            Employee employee = Employee.builder()
                    .employeeId("EMP001")
                    .name("Demo Employee")
                    .email("employee@ems.com")
                    .phone("9999999999")
                    .department("Engineering")
                    .designation("Software Engineer")
                    .joiningDate(LocalDate.now().minusMonths(6))
                    .status(EmployeeStatus.ACTIVE)
                    .user(empUser)
                    .build();
            employeeRepository.save(employee);
            log.info("Seeded demo EMPLOYEE user -> username: EMP001 | password: Employee@123");
        }
    }
}
