package com.group.booking.dto.response;

import lombok.Data;

@Data
public class RoomResponseDTO {
    private Long id;
    private String roomNumber;
    private String type;
    private Double price;
    private Integer capacity;
    private String status;
}