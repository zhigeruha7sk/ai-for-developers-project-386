package com.example.calendar.booking;

import static com.example.calendar.CalendarTime.ZONE;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.calendar.meeting.MeetingTypeCatalog;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Доступность слотов на «сегодня» через шов Clock: прошедшие старты недоступны, будущие свободны,
 * без зависимости от часа запуска тестов.
 */
class SlotAvailabilityServiceTest {

  /** Фиксированное «сейчас»: 11:15 в Asia/Almaty — между слотами 11:00 и 11:30. */
  private static final Instant FIXED_NOW = Instant.parse("2026-11-16T06:15:00Z");

  private final MeetingTypeCatalog meetingTypes = new MeetingTypeCatalog();
  private final BookingRepository bookings = new BookingRepository();
  private final LocalDate today = LocalDateTime.ofInstant(FIXED_NOW, ZONE).toLocalDate();

  private BookingService serviceAt(Instant now) {
    return new BookingService(meetingTypes, bookings, Clock.fixed(now, ZONE));
  }

  @Test
  void todaySlotsMarkPastStartsUnavailableAndFutureStartsAvailable() {
    List<Slot> slots = serviceAt(FIXED_NOW).slots("meeting-30", today);

    assertThat(slots).hasSize(16);
    for (Slot slot : slots) {
      if (slot.startTime().isBefore(LocalTime.of(11, 15))) {
        assertThat(slot.available()).as("слот %s уже прошёл", slot.startTime()).isFalse();
      } else {
        assertThat(slot.available()).as("слот %s ещё впереди", slot.startTime()).isTrue();
      }
    }
    // Граница «сейчас» лежит между 11:00 и 11:30: 11:00 прошёл, 11:30 — первый свободный.
    assertThat(slots.stream().filter(Slot::available).map(Slot::startTime).findFirst())
        .hasValue(LocalTime.of(11, 30));
  }

  @Test
  void todaySlotStartingExactlyAtNowRemainsAvailable() {
    // «Сейчас» ровно 11:00: старт в этот момент ещё не в прошлом.
    List<Slot> slots = serviceAt(Instant.parse("2026-11-16T06:00:00Z")).slots("meeting-30", today);

    Slot atNow =
        slots.stream()
            .filter(slot -> slot.startTime().equals(LocalTime.of(11, 0)))
            .findFirst()
            .orElseThrow();
    assertThat(atNow.available()).isTrue();
    assertThat(slots.stream().filter(Slot::available).map(Slot::startTime).findFirst())
        .hasValue(LocalTime.of(11, 0));
  }
}
