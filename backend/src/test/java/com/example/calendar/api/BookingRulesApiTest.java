package com.example.calendar.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

/**
 * Сценарии спеки, не закрытые BookingApiTest: прошлое время, частичные пересечения, границы рабочих
 * часов и окна записи, неизвестный тип встречи, заметка к брони.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookingRulesApiTest {

  private static final ZoneId ZONE = ZoneId.of("Asia/Almaty");

  @Autowired private Environment environment;

  private final HttpClient client =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  @Test
  void rejectsBookingOnPastDate() throws Exception {
    LocalDate yesterday = LocalDate.now(ZONE).minusDays(1);

    HttpResponse<String> response =
        post(
            "/api/bookings",
            booking("meeting-30", yesterday, "09:00", "Вчерашний гость", "🐸", null));

    assertThat(response.statusCode()).isEqualTo(400);
    assertThat(response.body()).contains("Дата встречи уже прошла");
  }

  @Test
  void rejectsBookingOnTodaySlotThatAlreadyPassed() throws Exception {
    LocalDate today = LocalDate.now(ZONE);
    LocalTime now = LocalTime.now(ZONE);
    assumeTrue(now.isAfter(LocalTime.of(9, 15)), "сегодня ещё нет прошедших рабочих слотов");

    HttpResponse<String> response =
        post(
            "/api/bookings", booking("meeting-30", today, "09:00", "Опоздавший гость", "🐨", null));

    assertThat(response.statusCode()).isEqualTo(400);
    assertThat(response.body()).contains("Это время уже прошло");
  }

  @Test
  void rejectsPartialOverlapBetweenDifferentMeetingTypes() throws Exception {
    LocalDate date = LocalDate.now(ZONE).plusDays(4);
    // Старты выровнены под длительность типа, поэтому пересечение двух типов — всегда частичное.
    HttpResponse<String> first =
        post("/api/bookings", booking("meeting-30", date, "10:00", "Первый гость", "🦊", null));
    assertThat(first.statusCode()).isEqualTo(200);

    HttpResponse<String> overlap =
        post("/api/bookings", booking("meeting-15", date, "10:15", "Налезший гость", "🐼", null));
    assertThat(overlap.statusCode()).isEqualTo(409);
  }

  @Test
  void rejectsPartialOverlapAgainstLongerMeetingType() throws Exception {
    LocalDate date = LocalDate.now(ZONE).plusDays(8);
    String longType =
        createMeetingType("Длинная встреча 60 минут", "Тип для проверки пересечений", 60);
    try {
      HttpResponse<String> longBooking =
          post("/api/bookings", booking(longType, date, "10:00", "Долгая встреча", "🐯", null));
      assertThat(longBooking.statusCode()).isEqualTo(200);

      // 10:00–11:00 против старта 10:30 — пересечение только наполовину.
      HttpResponse<String> overlap =
          post(
              "/api/bookings",
              booking("meeting-30", date, "10:30", "Половинный гость", "🦁", null));
      assertThat(overlap.statusCode()).isEqualTo(409);
    } finally {
      deleteMeetingType(longType);
    }
  }

  @Test
  void rejectsStartDuringLunchBreakAndAfterWorkingHours() throws Exception {
    LocalDate date = LocalDate.now(ZONE).plusDays(2);

    String[] starts = {"13:00", "13:30", "17:45", "18:00"};
    String[] types = {"meeting-30", "meeting-15", "meeting-30", "meeting-15"};
    for (int i = 0; i < starts.length; i++) {
      HttpResponse<String> response =
          post(
              "/api/bookings",
              booking(types[i], date, starts[i], "Гость " + starts[i], "🦉", null));
      assertThat(response.statusCode()).as("старт %s типом %s", starts[i], types[i]).isEqualTo(400);
      assertThat(response.body()).contains("Время выходит за рамки рабочих часов владельца");
    }
  }

  @Test
  void acceptsLastDayOfBookingWindowAndRejectsTheNextOne() throws Exception {
    LocalDate today = LocalDate.now(ZONE);
    // Окно записи — 14 календарных дней, включая сегодняшний: сегодня..сегодня+13.
    LocalDate lastDay = today.plusDays(13);

    HttpResponse<String> inside =
        post("/api/bookings", booking("meeting-30", lastDay, "11:00", "Крайний гость", "🐵", null));
    assertThat(inside.statusCode()).isEqualTo(200);

    HttpResponse<String> outside =
        post(
            "/api/bookings",
            booking("meeting-30", today.plusDays(14), "11:00", "Поздний гость", "🐙", null));
    assertThat(outside.statusCode()).isEqualTo(400);
    assertThat(outside.body()).contains("окно записи");
  }

  @Test
  void rejectsUnknownMeetingType() throws Exception {
    LocalDate date = LocalDate.now(ZONE).plusDays(3);

    HttpResponse<String> booking =
        post("/api/bookings", booking("meeting-unknown", date, "09:00", "Гость", "🦝", null));
    assertThat(booking.statusCode()).isEqualTo(404);
    assertThat(booking.body()).contains("Тип встречи не найден");

    HttpResponse<String> slots = get("/api/slots?meetingTypeId=meeting-unknown&date=" + date);
    assertThat(slots.statusCode()).isEqualTo(404);
  }

  @Test
  void savesOptionalNoteWhenProvided() throws Exception {
    LocalDate date = LocalDate.now(ZONE).plusDays(6);

    HttpResponse<String> withoutNote =
        post("/api/bookings", booking("meeting-30", date, "09:00", "Без заметки", "🐰", null));
    assertThat(withoutNote.statusCode()).isEqualTo(200);

    HttpResponse<String> withNote =
        post(
            "/api/bookings",
            booking("meeting-15", date, "10:00", "С заметкой", "🐻", "Перезвонить за 5 минут"));
    assertThat(withNote.statusCode()).isEqualTo(200);
    assertThat(withNote.body()).contains("Перезвонить за 5 минут");

    HttpResponse<String> upcoming = get("/api/bookings");
    assertThat(upcoming.statusCode()).isEqualTo(200);
    assertThat(upcoming.body()).contains("Перезвонить за 5 минут");
  }

  private String booking(
      String meetingTypeId,
      LocalDate date,
      String startTime,
      String guestName,
      String guestAvatar,
      String note) {
    String noteJson = note == null ? "" : ",\"note\":\"" + note + "\"";
    return "{\"meetingTypeId\":\""
        + meetingTypeId
        + "\",\"date\":\""
        + date
        + "\",\"startTime\":\""
        + startTime
        + "\",\"guestName\":\""
        + guestName
        + "\",\"guestAvatar\":\""
        + guestAvatar
        + "\""
        + noteJson
        + "}";
  }

  private String createMeetingType(String name, String description, int durationMinutes)
      throws Exception {
    HttpResponse<String> response =
        post(
            "/api/meeting-types",
            "{\"name\":\""
                + name
                + "\",\"description\":\""
                + description
                + "\",\"durationMinutes\":"
                + durationMinutes
                + "}");
    assertThat(response.statusCode()).isEqualTo(200);
    String body = response.body();
    int start = body.indexOf("\"id\":\"") + "\"id\":\"".length();
    return body.substring(start, body.indexOf('"', start));
  }

  private void deleteMeetingType(String id) throws Exception {
    HttpResponse<String> response = send("DELETE", "/api/meeting-types/" + id, null);
    assertThat(response.statusCode()).isEqualTo(204);
  }

  private HttpResponse<String> get(String path) throws Exception {
    return send("GET", path, null);
  }

  private HttpResponse<String> post(String path, String body) throws Exception {
    return send("POST", path, body);
  }

  private HttpResponse<String> send(String method, String path, String body) throws Exception {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(
                URI.create(
                    "http://localhost:" + environment.getProperty("local.server.port") + path))
            .timeout(Duration.ofSeconds(5));
    if (body == null) {
      builder.method(method, HttpRequest.BodyPublishers.noBody());
    } else {
      builder
          .method(method, HttpRequest.BodyPublishers.ofString(body))
          .header("Content-Type", "application/json; charset=utf-8");
    }
    return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
  }
}
