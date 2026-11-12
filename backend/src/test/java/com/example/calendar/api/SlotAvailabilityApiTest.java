package com.example.calendar.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

/**
 * Верхний шов: доступность слотов на «сегодня» и гонки бронирования. «Сегодня» фиксировано тестовым
 * Clock, поэтому сценарий не зависит от часа запуска.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SlotAvailabilityApiTest {

  private static final ZoneId ZONE = ZoneId.of("Asia/Almaty");

  /** Фиксированное «сейчас»: 11:15 в Asia/Almaty. */
  private static final Instant FIXED_NOW = Instant.parse("2026-11-16T06:15:00Z");

  private static final LocalDate TODAY = LocalDateTime.ofInstant(FIXED_NOW, ZONE).toLocalDate();

  @TestConfiguration
  static class FixedClockConfiguration {

    @Bean
    @Primary
    Clock fixedClock() {
      return Clock.fixed(FIXED_NOW, ZONE);
    }
  }

  @Autowired private Environment environment;

  private final HttpClient client =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  @Test
  void todaySlotsMarkPastStartsUnavailableAndFutureStartsAvailable() throws Exception {
    HttpResponse<String> response = get("/api/slots?meetingTypeId=meeting-30&date=" + TODAY);

    assertThat(response.statusCode()).isEqualTo(200);
    // Прошедшие старты недоступны, будущие свободны; граница — между 11:00 и 11:30.
    assertThat(response.body())
        .contains("\"startTime\":\"09:00:00\",\"endTime\":\"09:30:00\",\"available\":false")
        .contains("\"startTime\":\"11:00:00\",\"endTime\":\"11:30:00\",\"available\":false")
        .contains("\"startTime\":\"11:30:00\",\"endTime\":\"12:00:00\",\"available\":true")
        .contains("\"startTime\":\"14:00:00\",\"endTime\":\"14:30:00\",\"available\":true")
        .contains("\"startTime\":\"17:30:00\",\"endTime\":\"18:00:00\",\"available\":true")
        .doesNotContain("\"startTime\":\"13:00:00\"");
  }

  @Test
  void concurrentOverlappingBookingsAcrossMeetingTypesAllowExactlyOne() throws Exception {
    LocalDate date = TODAY.plusDays(1);
    // Каждый интервал накрывает 15:15–15:30, поэтому пересекается любая пара запросов.
    String[] bodies = {
      booking("meeting-30", date, "15:00", "Гонка 30", "🐺"),
      booking("meeting-15", date, "15:15", "Гонка 15", "🦊"),
      booking("meeting-30", date, "15:00", "Гонка 30 бис", "🐻"),
      booking("meeting-15", date, "15:15", "Гонка 15 бис", "🐼"),
    };

    CountDownLatch ready = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(bodies.length);
    List<Integer> statuses = new CopyOnWriteArrayList<>();
    ExecutorService pool = Executors.newFixedThreadPool(bodies.length);
    for (String body : bodies) {
      pool.submit(() -> {
        try {
          ready.await();
          statuses.add(post("/api/bookings", body).statusCode());
        } catch (Exception ignored) {
          statuses.add(500);
        } finally {
          done.countDown();
        }
      });
    }
    ready.countDown();
    assertThat(done.await(15, TimeUnit.SECONDS)).isTrue();
    pool.shutdown();

    assertThat(statuses).hasSize(bodies.length);
    assertThat(statuses.stream().filter(status -> status == 200).count()).isEqualTo(1);
    assertThat(statuses.stream().filter(status -> status == 409).count())
        .isEqualTo(bodies.length - 1L);
  }

  @Test
  void adjacentBookingStartingExactlyAtPreviousEndIsAllowed() throws Exception {
    LocalDate date = TODAY.plusDays(2);

    HttpResponse<String> first =
        post("/api/bookings", booking("meeting-30", date, "10:00", "Первый гость", "🦊"));
    assertThat(first.statusCode()).isEqualTo(200);

    // Старт ровно в момент окончания предыдущей встречи — не пересечение.
    HttpResponse<String> adjacent =
        post("/api/bookings", booking("meeting-15", date, "10:30", "Стыковой гость", "🦁"));
    assertThat(adjacent.statusCode()).isEqualTo(200);
    assertThat(adjacent.body())
        .contains("\"startTime\":\"10:30:00\"")
        .contains("\"endTime\":\"10:45:00\"");
  }

  private String booking(
      String meetingTypeId,
      LocalDate date,
      String startTime,
      String guestName,
      String guestAvatar) {
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
        + "\"}";
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
