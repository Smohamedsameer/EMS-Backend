package com.ems.service;

import com.ems.dto.HolidayRequest;
import com.ems.dto.HolidayResponse;
import com.ems.entity.Holiday;
import com.ems.exception.BadRequestException;
import com.ems.exception.DuplicateResourceException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HolidayService {

    private final HolidayRepository holidayRepository;

    @Transactional
    public HolidayResponse create(HolidayRequest request) {
        if (holidayRepository.existsByHolidayDate(request.getDate())) {
            throw new DuplicateResourceException("A holiday is already declared for " + request.getDate());
        }
        Holiday holiday = Holiday.builder()
                .holidayDate(request.getDate())
                .reason(request.getReason())
                .build();
        holiday = holidayRepository.save(holiday);
        return toResponse(holiday);
    }

    public List<HolidayResponse> getAll() {
        return holidayRepository.findAllByOrderByHolidayDateDesc().stream().map(this::toResponse).toList();
    }

    public List<HolidayResponse> getForMonth(YearMonth month) {
        return holidayRepository
                .findByHolidayDateBetweenOrderByHolidayDateAsc(month.atDay(1), month.atEndOfMonth())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public void delete(Long id) {
        if (!holidayRepository.existsById(id)) {
            throw new ResourceNotFoundException("Holiday not found with id: " + id);
        }
        holidayRepository.deleteById(id);
    }

    private HolidayResponse toResponse(Holiday h) {
        return HolidayResponse.builder()
                .id(h.getId())
                .date(h.getHolidayDate())
                .reason(h.getReason())
                .createdAt(h.getCreatedAt())
                .build();
    }
}
