package com.example.calendar.booking;

import static com.example.calendar.CalendarTime.WINDOW_DAYS;
import static com.example.calendar.CalendarTime.ZONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.calendar.meeting.MeetingType;
import com.example.calendar.meeting.MeetingTypeCatalog;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BookingServiceTest {

  /** Фиксированное «сейчас»: 10:00 в Asia/Almaty. */
  private static final Instant FIXED_NOW = Instant.parse("2026-11-12T05:00:00Z");

  private MeetingTypeCatalog meetingTypes;
  private BookingRepository bookings;
  private BookingService service;
  private LocalDate today;

  @BeforeEach
  void setUp() {
    meetingTypes = new MeetingTypeCatalog();
    bookings = new BookingRepository();
    Clock clock = Clock.fixed(FIXED_NOW, ZONE);
    service = new BookingService(meetingTypes, bookings, clock);
    today = LocalDateTime.ofInstant(FIXED_NOW, ZONE).toLocalDate();
  }

  @Test
  void thirtyMinuteSlotsUseDurationAsStepAndRespectWorkingHours() {
    List<Slot> slots = service.slots("meeting-30", today.plusDays(1));

    assertThat(slots).hasSize(16);
    assertThat(slots)
        .allSatisfy(slot -> assertThat(slot.endTime()).isEqualTo(slot.startTime().plusMinutes(30)));
    List<LocalTime> starts = slots.stream().map(Slot::startTime).toList();
    assertThat(starts)
        .contains(
            LocalTime.of(9, 0), LocalTime.of(12, 30), LocalTime.of(14, 0), LocalTime.of(17, 30))
        .doesNotContain(LocalTime.of(13, 0), LocalTime.of(18, 0));
  }

  @Test
  void fifteenMinuteSlotsFillWorkingHoursWithDurationStep() {
    List<Slot> slots = service.slots("meeting-15", today.plusDays(1));

    assertThat(slots).hasSize(32);
    assertThat(slots)
        .allSatisfy(slot -> assertThat(slot.endTime()).isEqualTo(slot.startTime().plusMinutes(15)));
    List<LocalTime> starts = slots.stream().map(Slot::startTime).toList();
    assertThat(starts)
        .contains(
            LocalTime.of(9, 0), LocalTime.of(12, 45), LocalTime.of(14, 0), LocalTime.of(17, 45))
        .doesNotContain(LocalTime.of(13, 0), LocalTime.of(18, 0));
  }

  @Test
  void sixtyMinuteSlotsStartOnTheHourWithinWorkingPeriods() {
    MeetingType sixty = meetingTypes.create("Встреча 60 минут", "Часовой шаг", 60);

    List<Slot> slots = service.slots(sixty.id(), today.plusDays(1));

    List<LocalTime> starts = slots.stream().map(Slot::startTime).toList();
    assertThat(starts)
        .containsExactly(
            LocalTime.of(9, 0),
            LocalTime.of(10, 0),
            LocalTime.of(11, 0),
            LocalTime.of(12, 0),
            LocalTime.of(14, 0),
            LocalTime.of(15, 0),
            LocalTime.of(16, 0),
            LocalTime.of(17, 0));
    assertThat(slots)
        .allSatisfy(slot -> assertThat(slot.endTime()).isEqualTo(slot.startTime().plusHours(1)));
  }

  @Test
  void fortyFiveMinuteSlotsStopAtWorkingIntervalEnd() {
    MeetingType fortyFive = meetingTypes.create("Встреча 45 минут", "Нестандартный шаг", 45);

    List<Slot> slots = service.slots(fortyFive.id(), today.plusDays(1));

    List<LocalTime> starts = slots.stream().map(Slot::startTime).toList();
    assertThat(starts)
        .containsExactly(
            LocalTime.of(9, 0),
            LocalTime.of(9, 45),
            LocalTime.of(10, 30),
            LocalTime.of(11, 15),
            LocalTime.of(12, 0),
            LocalTime.of(14, 0),
            LocalTime.of(14, 45),
            LocalTime.of(15, 30),
            LocalTime.of(16, 15),
            LocalTime.of(17, 0));
    assertThat(slots)
        .allSatisfy(slot -> assertThat(slot.endTime()).isBeforeOrEqualTo(LocalTime.of(18, 0)));
  }

  @Test
  void createRejectsStartTimeThatIsNotAlignedWithDurationStep() {
    assertThatThrownBy(() ->
            service.create(
                "meeting-30", today.plusDays(1), LocalTime.of(9, 10), "Гость", "🦊", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("рабочих часов");
  }

  @Test
  void createRejectsPastTimeOnTodayAndAcceptsCurrentBoundary() {
    assertThatThrownBy(() ->
            service.create("meeting-30", today, LocalTime.of(9, 30), "Опоздавший", "🐨", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Это время уже прошло");

    Booking atCurrentTime =
        service.create("meeting-30", today, LocalTime.of(10, 0), "Сейчас", "🦉", null);
    assertThat(atCurrentTime.startTime()).isEqualTo(LocalTime.of(10, 0));
  }

  @Test
  void createAcceptsLastWindowDayAndRejectsTheDayAfter() {
    LocalDate lastDay = today.plusDays(WINDOW_DAYS - 1L);

    Booking booking =
        service.create("meeting-30", lastDay, LocalTime.of(11, 0), "Крайний гость", "🐵", null);
    assertThat(booking.date()).isEqualTo(lastDay);

    assertThatThrownBy(() ->
            service.create(
                "meeting-30",
                today.plusDays(WINDOW_DAYS),
                LocalTime.of(11, 0),
                "Поздний гость",
                "🐙",
                null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("окно записи");
  }

  @Test
  void createRejectsPartialOverlapAcrossTypesButAllowsAdjacentStart() {
    LocalDate date = today.plusDays(2);
    service.create("meeting-30", date, LocalTime.of(10, 0), "Первый", "🦊", null);

    assertThatThrownBy(
            () -> service.create("meeting-15", date, LocalTime.of(10, 15), "Наложение", "🐼", null))
        .isInstanceOf(SlotUnavailableException.class);

    Booking adjacent =
        service.create("meeting-15", date, LocalTime.of(10, 30), "Стыковой", "🦁", null);
    assertThat(adjacent.startTime()).isEqualTo(LocalTime.of(10, 30));
  }

  @Test
  void upcomingFiltersPastBookingsAndSortsByDateThenStartTime() {
    bookings.add(
        new Booking(
            "past",
            "meeting-15",
            "Встреча 15 минут",
            today,
            LocalTime.of(9, 0),
            LocalTime.of(9, 15),
            "Прошлый гость",
            "🐸",
            null));
    service.create("meeting-30", today.plusDays(1), LocalTime.of(9, 0), "Завтра", "🐯", null);
    service.create("meeting-30", today, LocalTime.of(11, 0), "Позже сегодня", "🐰", null);
    service.create("meeting-15", today, LocalTime.of(10, 30), "Раньше сегодня", "🐻", null);

    List<Booking> upcoming = service.upcoming();

    assertThat(upcoming).extracting(Booking::guestName).doesNotContain("Прошлый гость");
    assertThat(upcoming)
        .extracting(Booking::guestName)
        .containsExactly("Раньше сегодня", "Позже сегодня", "Завтра");
  }
}
