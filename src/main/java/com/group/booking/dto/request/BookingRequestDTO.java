
package com.group.booking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.FutureOrPresent;
import lombok.Data;
import java.time.LocalDate;

@Data
@Schema(description = "Request data for creating a hotel booking")
public class BookingRequestDTO {

    @NotNull(message = "ID không được để trống!")
    @Schema(
        description = "ID of the room to book",
        example = "1"
    )
    private Long roomId;

    @NotNull(message = "Ngày nhận phòng không được để trống!")
    @FutureOrPresent(message = "Ngày nhận phòng không được ở quá khứ!")
    @Schema(
        description = "Check-in date (YYYY-MM-DD)",
        example = "2026-12-01"
    )
    private LocalDate checkInDate;

    @NotNull(message = "Ngày trả phòng không được để trống!")
    @FutureOrPresent(message = "Ngày trả phòng không được ở quá khứ!")
    @Schema(
        description = "Check-out date (YYYY-MM-DD), must be after check-in date",
        example = "2026-12-03"
    )
    private LocalDate checkOutDate;
}
