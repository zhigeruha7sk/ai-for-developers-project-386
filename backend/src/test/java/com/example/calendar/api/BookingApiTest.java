package com.example.calendar.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookingApiTest {

  private static final ZoneId ZONE = ZoneId.of("Asia/Almaty");

  @Autowired private Environment environment;

  private final HttpClient client =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  @Test
  void slotsForFifteenMinutesCoverBothWorkingPeriods() throws Exception {
    LocalDate today = LocalDate.now(ZONE);
    HttpResponse<String> response = get("/api/slots?meetingTypeId=meeting-15&date=" + today);

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(occurs(response.body(), "\"startTime\"")).isEqualTo(32);
    assertThat(response.body())
        .contains("\"startTime\":\"09:00:00\"")
        .contains("\"startTime\":\"12:45:00\"")
        .contains("\"startTime\":\"14:00:00\"")
        .contains("\"startTime\":\"17:45:00\"");
  }

  @Test
  void bookingMarksSlotBusyAndRejectsOverlapButAllowsAdjacentSlot() throws Exception {
    LocalDate date = LocalDate.now(ZONE).plusDays(5);
    String bookingJson =
        "{\"meetingTypeId\":\"meeting-30\",\"date\":\""
            + date
            + "\",\"startTime\":\"09:00\",\"guestName\":\"Иван Тестов\",\"guestAvatar\":\"🦊\"}";

    HttpResponse<String> first = post("/api/bookings", bookingJson);
    assertThat(first.statusCode()).isEqualTo(200);
    assertThat(first.body())
        .contains("\"guestName\":\"Иван Тестов\"")
        .contains("\"startTime\":\"09:00:00\"");

    HttpResponse<String> slots = get("/api/slots?meetingTypeId=meeting-30&date=" + date);
    assertThat(slots.statusCode()).isEqualTo(200);
    assertThat(slots.body())
        .contains("\"startTime\":\"09:00:00\",\"endTime\":\"09:30:00\",\"available\":false");

    HttpResponse<String> overlap = post("/api/bookings", bookingJson);
    assertThat(overlap.statusCode()).isEqualTo(409);

    HttpResponse<String> adjacent =
        post(
            "/api/bookings",
            "{\"meetingTypeId\":\"meeting-30\",\"date\":\""
                + date
                + "\",\"startTime\":\"09:30\",\"guestName\":\"Мария Гость\",\"guestAvatar\":\"🐼\"}");
    assertThat(adjacent.statusCode()).isEqualTo(200);

    HttpResponse<String> upcoming = get("/api/bookings");
    assertThat(upcoming.statusCode()).isEqualTo(200);
    assertThat(upcoming.body()).contains("\"guestName\":\"Иван Тестов\"");
  }

  @Test
  void rejectsStartTimeOutsideWorkingHours() throws Exception {
    LocalDate date = LocalDate.now(ZONE).plusDays(5);
    HttpResponse<String> response =
        post(
            "/api/bookings",
            "{\"meetingTypeId\":\"meeting-30\",\"date\":\""
                + date
                + "\",\"startTime\":\"08:00\",\"guestName\":\"Тест\",\"guestAvatar\":\"🦉\"}");
    assertThat(response.statusCode()).isEqualTo(400);
  }

  @Test
  void rejectsDateOutsideBookingWindow() throws Exception {
    LocalDate date = LocalDate.now(ZONE).plusDays(20);
    HttpResponse<String> response =
        post(
            "/api/bookings",
            "{\"meetingTypeId\":\"meeting-30\",\"date\":\""
                + date
                + "\",\"startTime\":\"09:00\",\"guestName\":\"Тест\",\"guestAvatar\":\"🦉\"}");
    assertThat(response.statusCode()).isEqualTo(400);
  }

  @Test
  void simultaneousBookingsForSameSlotAllowOnlyOne() throws Exception {
    LocalDate date = LocalDate.now(ZONE).plusDays(7);
    String body =
        "{\"meetingTypeId\":\"meeting-30\",\"date\":\""
            + date
            + "\",\"startTime\":\"10:00\",\"guestName\":\"Гонка\",\"guestAvatar\":\"🐺\"}";

    CountDownLatch ready = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(2);
    List<Integer> statuses = new CopyOnWriteArrayList<>();
    ExecutorService pool = Executors.newFixedThreadPool(2);

    Runnable book =
        () -> {
          try {
            ready.await();
            statuses.add(post("/api/bookings", body).statusCode());
          } catch (Exception ignored) {
            statuses.add(500);
          } finally {
            done.countDown();
          }
        };

    pool.submit(book);
    pool.submit(book);
    ready.countDown();
    assertThat(done.await(15, TimeUnit.SECONDS)).isTrue();
    pool.shutdown();

    assertThat(statuses).hasSize(2);
    assertThat(statuses.stream().filter(status -> status == 200).count()).isEqualTo(1);
    assertThat(statuses.stream().filter(status -> status == 409).count()).isEqualTo(1);
  }

  private HttpResponse<String> get(String path) throws Exception {
    return client.send(
        HttpRequest.newBuilder(
                URI.create(
                    "http://localhost:" + environment.getProperty("local.server.port") + path))
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> post(String path, String body) throws Exception {
    return client.send(
        HttpRequest.newBuilder(
                URI.create(
                    "http://localhost:" + environment.getProperty("local.server.port") + path))
            .timeout(Duration.ofSeconds(5))
            .header("Content-Type", "application/json; charset=utf-8")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private long occurs(String body, String token) {
    return body.split(token, -1).length - 1L;
  }
}
