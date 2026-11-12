package com.example.calendar.booking;

import static com.example.calendar.CalendarTime.AFTERNOON_END;
import static com.example.calendar.CalendarTime.AFTERNOON_START;
import static com.example.calendar.CalendarTime.MORNING_END;
import static com.example.calendar.CalendarTime.MORNING_START;
import static com.example.calendar.CalendarTime.WINDOW_DAYS;
import static com.example.calendar.CalendarTime.ZONE;

import com.example.calendar.meeting.MeetingType;
import com.example.calendar.meeting.MeetingTypeCatalog;
import com.example.calendar.meeting.MeetingTypeNotFoundException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Service;

/** Логика слотов и записи на встречи. */
@Service
public class BookingService {

  private static final List<Interval> WORKING_INTERVALS =
      List.of(
          new Interval(MORNING_START, MORNING_END), new Interval(AFTERNOON_START, AFTERNOON_END));

  private final MeetingTypeCatalog meetingTypes;
  private final BookingRepository bookings;
  private final Clock clock;
  private final ReentrantLock lock = new ReentrantLock();

  public BookingService(MeetingTypeCatalog meetingTypes, BookingRepository bookings, Clock clock) {
    this.meetingTypes = meetingTypes;
    this.bookings = bookings;
    this.clock = clock;
  }

  /** Слоты выбранного типа на дату, с пометкой о доступности. */
  public List<Slot> slots(String meetingTypeId, LocalDate date) {
    MeetingType type = meetingTypeById(meetingTypeId);
    int step = type.durationMinutes();
    List<Slot> result = new ArrayList<>();
    for (Interval interval : WORKING_INTERVALS) {
      LocalTime start = interval.start();
      while (!start.plusMinutes(step).isAfter(interval.end())) {
        LocalTime end = start.plusMinutes(step);
        boolean available =
            bookings.findOverlapping(date, start, end).isEmpty() && !isPast(date, start);
        result.add(new Slot(start, end, available));
        start = end;
      }
    }
    return result;
  }

  /** Создаёт бронь, проверяя окно записи, рабочие часы и отсутствие пересечений. */
  public Booking create(
      String meetingTypeId,
      LocalDate date,
      LocalTime startTime,
      String guestName,
      String guestAvatar,
      String note) {
    MeetingType type = meetingTypeById(meetingTypeId);
    int step = type.durationMinutes();

    LocalDate today = LocalDate.now(clock);
    if (date.isBefore(today)) {
      throw new IllegalArgumentException("Дата встречи уже прошла");
    }
    if (date.isAfter(today.plusDays(WINDOW_DAYS - 1L))) {
      throw new IllegalArgumentException("Дата выходит за окно записи в " + WINDOW_DAYS + " дней");
    }
    if (!isWorkingSlot(startTime, step)) {
      throw new IllegalArgumentException("Время выходит за рамки рабочих часов владельца");
    }
    if (isPast(date, startTime)) {
      throw new IllegalArgumentException("Это время уже прошло");
    }

    LocalTime end = startTime.plusMinutes(step);

    lock.lock();
    try {
      if (!bookings.findOverlapping(date, startTime, end).isEmpty()) {
        throw new SlotUnavailableException();
      }
      Booking booking =
          new Booking(
              UUID.randomUUID().toString(),
              type.id(),
              type.name(),
              date,
              startTime,
              end,
              guestName,
              guestAvatar,
              note);
      bookings.add(booking);
      return booking;
    } finally {
      lock.unlock();
    }
  }

  /** Предстоящие встречи, отсортированные по дате и времени. */
  public List<Booking> upcoming() {
    return bookings.findAll().stream()
        .filter(booking -> !isPast(booking.date(), booking.startTime()))
        .sorted(Comparator.comparing(Booking::date).thenComparing(Booking::startTime))
        .toList();
  }

  private MeetingType meetingTypeById(String id) {
    return meetingTypes.findById(id).orElseThrow(() -> new MeetingTypeNotFoundException(id));
  }

  private boolean isPast(LocalDate date, LocalTime time) {
    return ZonedDateTime.of(date, time, ZONE).isBefore(ZonedDateTime.now(clock));
  }

  private boolean isWorkingSlot(LocalTime start, int step) {
    for (Interval interval : WORKING_INTERVALS) {
      if (!start.isBefore(interval.start()) && !start.plusMinutes(step).isAfter(interval.end())) {
        long offsetMinutes = Duration.between(interval.start(), start).toMinutes();
        return offsetMinutes % step == 0;
      }
    }
    return false;
  }

  private record Interval(LocalTime start, LocalTime end) {}
}
