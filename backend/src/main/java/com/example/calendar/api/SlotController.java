package com.example.calendar.api;

import com.example.calendar.booking.BookingService;
import com.example.calendar.booking.Slot;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SlotController {

  private final BookingService bookingService;

  public SlotController(BookingService bookingService) {
    this.bookingService = bookingService;
  }

  @GetMapping("/slots")
  public List<Slot> list(
      @RequestParam String meetingTypeId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return bookingService.slots(meetingTypeId, date);
  }
}
