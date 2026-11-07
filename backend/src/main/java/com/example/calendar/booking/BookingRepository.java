package com.example.calendar.booking;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Repository;

/** Хранилище броней в памяти процесса. Потокобезопасно для чтения. */
@Repository
public class BookingRepository {

  private final CopyOnWriteArrayList<Booking> bookings = new CopyOnWriteArrayList<>();

  public List<Booking> findAll() {
    return List.copyOf(bookings);
  }

  public void add(Booking booking) {
    bookings.add(booking);
  }

  public List<Booking> findOverlapping(LocalDate date, LocalTime start, LocalTime end) {
    return bookings.stream()
        .filter(
            b -> b.date().equals(date) && b.startTime().isBefore(end) && b.endTime().isAfter(start))
        .toList();
  }
}
