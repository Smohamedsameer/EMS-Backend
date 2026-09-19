package com.ems.controller;

import com.ems.dto.HolidayRequest;
import com.ems.dto.HolidayResponse;
import com.ems.service.HolidayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/holidays")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayService holidayService;

    /** Any authenticated user (admin or employee) can view declared holidays. */
    @GetMapping
    public ResponseEntity<List<HolidayResponse>> getAll(@RequestParam(required = false) String month) {
        if (month != null && !month.isBlank()) {
            return ResponseEntity.ok(holidayService.getForMonth(YearMonth.parse(month)));
        }
        return ResponseEntity.ok(holidayService.getAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HolidayResponse> create(@Valid @RequestBody HolidayRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(holidayService.create(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        holidayService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
