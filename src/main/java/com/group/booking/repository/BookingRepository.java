package com.group.booking.repository;

import com.group.booking.model.Booking;
import com.group.booking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUser(User user);
}