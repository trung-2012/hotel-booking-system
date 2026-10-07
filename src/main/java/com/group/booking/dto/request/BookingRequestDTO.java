package com.group.booking.dto.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class BookingRequestDTO {

    private Long roomId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
}