package com.ems.service;

import com.ems.dto.LoginRequest;
import com.ems.dto.LoginResponse;
import com.ems.entity.Employee;
import com.ems.entity.User;
import com.ems.repository.EmployeeRepository;
import com.ems.repository.UserRepository;
import com.ems.security.CustomUserDetails;
import com.ems.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail(), request.getPassword())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name(), user.getId());

        LoginResponse.LoginResponseBuilder builder = LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name());

        if (user.getRole().name().equals("EMPLOYEE")) {
            Optional<Employee> employee = employeeRepository.findByUserId(user.getId());
            employee.ifPresent(e -> builder
                    .employeeDbId(e.getId())
                    .employeeId(e.getEmployeeId())
                    .name(e.getName()));
        } else {
            builder.name(user.getUsername());
        }

        return builder.build();
    }
}
