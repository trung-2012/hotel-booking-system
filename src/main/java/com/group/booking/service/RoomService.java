package com.group.booking.service;

import com.group.booking.dto.request.RoomRequestDTO;
import com.group.booking.dto.response.RoomResponseDTO;
import com.group.booking.model.Room;
import com.group.booking.model.RoomStatus;
import com.group.booking.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;

    public List<RoomResponseDTO> getAllRooms() {
        return roomRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public RoomResponseDTO createRoom(RoomRequestDTO dto) {
        if (roomRepository.existsByRoomNumber(dto.getRoomNumber())) {
            throw new RuntimeException("Số phòng đã tồn tại!");
        }
        Room room = new Room();
        room.setRoomNumber(dto.getRoomNumber());
        room.setType(dto.getType());
        room.setPrice(dto.getPrice());
        room.setCapacity(dto.getCapacity());
        room.setStatus(RoomStatus.AVAILABLE);
        return toDTO(roomRepository.save(room));
    }

    public void deleteRoom(Long id) {
        if (!roomRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy phòng!");
        }
        roomRepository.deleteById(id);
    }

    private RoomResponseDTO toDTO(Room room) {
        RoomResponseDTO dto = new RoomResponseDTO();
        dto.setId(room.getId());
        dto.setRoomNumber(room.getRoomNumber());
        dto.setType(room.getType());
        dto.setPrice(room.getPrice());
        dto.setCapacity(room.getCapacity());
        dto.setStatus(room.getStatus() != null ? room.getStatus().name() : null);
        return dto;
    }
}