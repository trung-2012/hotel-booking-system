package com.group.booking.controller;

import com.group.booking.dto.request.BookingRequestDTO;
import com.group.booking.dto.response.BookingResponseDTO;
import com.group.booking.service.BookingService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public BookingResponseDTO createBooking(
            @RequestBody BookingRequestDTO request,
            Authentication authentication) {

        String email = authentication.getName();

        return bookingService.createBooking(email, request);
    }

    @GetMapping("/me")
    public List<BookingResponseDTO> getMyBookings(
            Authentication authentication) {

        String email = authentication.getName();

        return bookingService.getBookingsByUser(email);
    }
}