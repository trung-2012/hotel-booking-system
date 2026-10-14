package com.group.booking.dto.response;

import lombok.Data;
import java.time.LocalDate;

@Data
public class BookingResponseDTO {

    private Long id;
    private Long roomId;
    private String roomNumber;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Double totalPrice;
}