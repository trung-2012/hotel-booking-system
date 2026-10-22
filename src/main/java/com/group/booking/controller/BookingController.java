package com.group.booking.controller;

import com.group.booking.dto.request.BookingRequestDTO;
import com.group.booking.dto.response.BookingResponseDTO;
import com.group.booking.service.BookingService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Booking API", description = "APIs for managing hotel bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Operation(summary = "Create a new booking", description = "Create a hotel room booking for an authenticated user")
    @ApiResponse(responseCode = "200", description = "Booking created successfully")
    @PostMapping
    public BookingResponseDTO createBooking(
            @RequestBody BookingRequestDTO request,
            Authentication authentication) {

        String email = authentication.getName();

        return bookingService.createBooking(email, request);
    }

    @Operation(summary = "Get my bookings", description = "Retrieve all bookings belonging to the authenticated user")
    @ApiResponse(responseCode = "200", description = "Booking list retrieved successfully")
    @GetMapping("/me")
    public List<BookingResponseDTO> getMyBookings(
            Authentication authentication) {

        String email = authentication.getName();

        return bookingService.getBookingsByUser(email);
    }

    // GET /api/bookings/{id} - Get booking details
    @Operation(summary = "Get booking details", description = "Retrieve booking details by ID. Only the booking owner can access it")
    @ApiResponse(responseCode = "200", description = "Booking details retrieved successfully")
    @GetMapping("/{id}")
    public BookingResponseDTO getBookingById(
            @PathVariable Long id,
            Authentication authentication) {

        String email = authentication.getName();

        return bookingService.getBookingById(email, id);
    }

    // DELETE /api/bookings/{id} - Cancel booking
    @Operation(summary = "Cancel a booking", description = "Cancel a booking by ID. Only the booking owner can cancel it")
    @ApiResponse(responseCode = "204", description = "Booking cancelled successfully")
    @ApiResponse(responseCode = "404", description = "Booking not found or access denied")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelBooking(
            @PathVariable Long id,
            Authentication authentication) {

        String email = authentication.getName();

        bookingService.cancelBooking(email, id);

        return ResponseEntity.noContent().build();
    }

}