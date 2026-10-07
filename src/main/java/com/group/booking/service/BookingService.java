package com.group.booking.service;

import com.group.booking.dto.request.BookingRequestDTO;
import com.group.booking.dto.response.BookingResponseDTO;
import com.group.booking.model.Booking;
import com.group.booking.model.Room;
import com.group.booking.model.User;
import com.group.booking.repository.BookingRepository;
import com.group.booking.repository.RoomRepository;
import com.group.booking.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    public BookingService(
            BookingRepository bookingRepository,
            RoomRepository roomRepository,
            UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    public BookingResponseDTO createBooking(
            String email,
            BookingRequestDTO request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (request.getCheckInDate() == null ||
                request.getCheckOutDate() == null ||
                !request.getCheckOutDate().isAfter(request.getCheckInDate())) {
            throw new IllegalArgumentException(
                    "Check-out date must be after check-in date");
        }

        long numberOfNights = ChronoUnit.DAYS.between(
                request.getCheckInDate(),
                request.getCheckOutDate());

        double totalPrice = room.getPrice() * numberOfNights;

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setRoom(room);
        booking.setCheckInDate(request.getCheckInDate());
        booking.setCheckOutDate(request.getCheckOutDate());
        booking.setTotalPrice(totalPrice);

        Booking savedBooking = bookingRepository.save(booking);

        return toResponseDTO(savedBooking);
    }

    public List<BookingResponseDTO> getBookingsByUser(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return bookingRepository.findByUser(user)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    private BookingResponseDTO toResponseDTO(Booking booking) {

        BookingResponseDTO response = new BookingResponseDTO();

        response.setId(booking.getId());
        response.setRoomId(booking.getRoom().getId());
        response.setRoomNumber(booking.getRoom().getRoomNumber());
        response.setCheckInDate(booking.getCheckInDate());
        response.setCheckOutDate(booking.getCheckOutDate());
        response.setTotalPrice(booking.getTotalPrice());

        return response;
    }
}