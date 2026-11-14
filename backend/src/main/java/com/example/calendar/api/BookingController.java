package com.example.calendar.api;

import com.example.calendar.booking.Booking;
import com.example.calendar.booking.BookingService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class BookingController {

  private final BookingService bookingService;

  public BookingController(BookingService bookingService) {
    this.bookingService = bookingService;
  }

  @GetMapping("/bookings")
  public List<Booking> list() {
    return bookingService.upcoming();
  }

  @PostMapping("/bookings")
  public Booking create(@RequestBody CreateBookingRequest request) {
    if (request.meetingTypeId() == null || request.meetingTypeId().isBlank()) {
      throw new IllegalArgumentException("Идентификатор типа встречи не должен быть пустым");
    }
    if (request.date() == null) {
      throw new IllegalArgumentException("Дата встречи не должна быть пустой");
    }
    if (request.startTime() == null) {
      throw new IllegalArgumentException("Время начала встречи не должно быть пустым");
    }
    if (request.guestName() == null || request.guestName().isBlank()) {
      throw new IllegalArgumentException("Имя гостя не должно быть пустым");
    }
    if (request.guestAvatar() == null || request.guestAvatar().isBlank()) {
      throw new IllegalArgumentException("Аватар гостя не должен быть пустым");
    }
    return bookingService.create(
        request.meetingTypeId(),
        request.date(),
        request.startTime(),
        request.guestName(),
        request.guestAvatar(),
        request.note());
  }
}
