package com.group.booking.dto.request;

import lombok.Data;

@Data
public class RoomRequestDTO {
    private String roomNumber;
    private String type;
    private Double price;
    private Integer capacity;
}
