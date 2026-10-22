
package com.group.booking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDate;

@Data
@Schema(description = "Request data for creating a hotel booking")
public class BookingRequestDTO {

    @Schema(
        description = "ID of the room to book",
        example = "1"
    )
    private Long roomId;

    @Schema(
        description = "Check-in date (YYYY-MM-DD)",
        example = "2026-12-01"
    )
    private LocalDate checkInDate;

    @Schema(
        description = "Check-out date (YYYY-MM-DD), must be after check-in date",
        example = "2026-12-03"
    )
    private LocalDate checkOutDate;
}
