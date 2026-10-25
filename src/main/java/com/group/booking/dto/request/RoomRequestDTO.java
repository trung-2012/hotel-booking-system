package com.group.booking.dto.request;

import lombok.Data;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class RoomRequestDTO {
    @NotBlank(message = "Số phòng không được để trống!")
    private String roomNumber;

    @NotBlank(message = "Loại phòng không được để trống!")
    private String type;

    @NotNull(message = "Giá phòng không được để trống!")
    @Min(value = 0, message = "Giá phòng không được là số âm!")
    private Double price;

    @NotNull(message = "Sức chứa không được để trống")
    @Min(value = 1, message = "Sức chứa phải ít nhất là 1 người")
    private Integer capacity;
}
