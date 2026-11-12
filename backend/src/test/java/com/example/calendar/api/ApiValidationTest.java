package com.example.calendar.api;

import static com.example.calendar.CalendarTime.ZONE;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiValidationTest {

  @Autowired private Environment environment;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private final HttpClient client =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  @Test
  void rejectsBlankNameWhenCreatingMeetingType() throws Exception {
    assertThat(
            post(
                    "/api/meeting-types",
                    "{\"name\":\"\",\"description\":\"Описание\",\"durationMinutes\":30}")
                .statusCode())
        .isEqualTo(400);
    assertThat(
            post(
                    "/api/meeting-types",
                    "{\"name\":\"   \",\"description\":\"Описание\",\"durationMinutes\":30}")
                .statusCode())
        .isEqualTo(400);
  }

  @Test
  void rejectsBlankDescriptionWhenCreatingMeetingType() throws Exception {
    assertThat(
            post(
                    "/api/meeting-types",
                    "{\"name\":\"Название\",\"description\":\"\",\"durationMinutes\":30}")
                .statusCode())
        .isEqualTo(400);
    assertThat(
            post(
                    "/api/meeting-types",
                    "{\"name\":\"Название\",\"description\":\"  \",\"durationMinutes\":30}")
                .statusCode())
        .isEqualTo(400);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 4, 241, 480})
  void rejectsDurationOutsideRangeWhenCreatingMeetingType(int durationMinutes) throws Exception {
    HttpResponse<String> response =
        post(
            "/api/meeting-types",
            "{\"name\":\"Название\",\"description\":\"Описание\",\"durationMinutes\":"
                + durationMinutes
                + "}");
    assertThat(response.statusCode()).isEqualTo(400);
  }

  @Test
  void acceptsBoundaryDurationsWhenCreatingMeetingType() throws Exception {
    HttpResponse<String> shortest =
        post(
            "/api/meeting-types",
            "{\"name\":\"Пять минут\",\"description\":\"Граница диапазона\","
                + "\"durationMinutes\":5}");
    assertThat(shortest.statusCode()).isEqualTo(200);

    HttpResponse<String> longest =
        post(
            "/api/meeting-types",
            "{\"name\":\"Четыре часа\",\"description\":\"Граница диапазона\","
                + "\"durationMinutes\":240}");
    assertThat(longest.statusCode()).isEqualTo(200);

    delete("/api/meeting-types/" + extractId(shortest.body()));
    delete("/api/meeting-types/" + extractId(longest.body()));
  }

  @Test
  void rejectsBlankFieldsWhenUpdatingMeetingType() throws Exception {
    assertThat(
            put(
                    "/api/meeting-types/meeting-15",
                    "{\"name\":\"\",\"description\":\"Описание\",\"durationMinutes\":30}")
                .statusCode())
        .isEqualTo(400);
    assertThat(
            put(
                    "/api/meeting-types/meeting-15",
                    "{\"name\":\"Название\",\"description\":\" \",\"durationMinutes\":30}")
                .statusCode())
        .isEqualTo(400);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 4, 241, 480})
  void rejectsDurationOutsideRangeWhenUpdatingMeetingType(int durationMinutes) throws Exception {
    HttpResponse<String> response =
        put(
            "/api/meeting-types/meeting-15",
            "{\"name\":\"Название\",\"description\":\"Описание\",\"durationMinutes\":"
                + durationMinutes
                + "}");
    assertThat(response.statusCode()).isEqualTo(400);
  }

  @Test
  void returnsNotFoundWhenUpdatingUnknownMeetingType() throws Exception {
    HttpResponse<String> response =
        put(
            "/api/meeting-types/does-not-exist",
            "{\"name\":\"Название\",\"description\":\"Описание\",\"durationMinutes\":30}");
    assertThat(response.statusCode()).isEqualTo(404);
  }

  @Test
  void returnsNoContentWhenDeletingUnknownMeetingType() throws Exception {
    HttpResponse<String> response = delete("/api/meeting-types/does-not-exist");
    assertThat(response.statusCode()).isEqualTo(204);
  }

  @Test
  void rejectsBlankGuestNameWhenBooking() throws Exception {
    LocalDate date = LocalDate.now(ZONE).plusDays(5);
    assertThat(
            post(
                    "/api/bookings",
                    "{\"meetingTypeId\":\"meeting-30\",\"date\":\""
                        + date
                        + "\",\"startTime\":\"09:00\",\"guestName\":\"\",\"guestAvatar\":\"🦊\"}")
                .statusCode())
        .isEqualTo(400);
    assertThat(
            post(
                    "/api/bookings",
                    "{\"meetingTypeId\":\"meeting-30\",\"date\":\""
                        + date
                        + "\",\"startTime\":\"09:00\",\"guestName\":\"  \",\"guestAvatar\":\"🦊\"}")
                .statusCode())
        .isEqualTo(400);
  }

  @Test
  void rejectsBlankGuestAvatarWhenBooking() throws Exception {
    LocalDate date = LocalDate.now(ZONE).plusDays(5);
    assertThat(
            post(
                    "/api/bookings",
                    "{\"meetingTypeId\":\"meeting-30\",\"date\":\""
                        + date
                        + "\",\"startTime\":\"09:00\",\"guestName\":\"Иван\",\"guestAvatar\":\"\"}")
                .statusCode())
        .isEqualTo(400);
    assertThat(
            post(
                    "/api/bookings",
                    "{\"meetingTypeId\":\"meeting-30\",\"date\":\""
                        + date
                        + "\",\"startTime\":\"09:00\",\"guestName\":\"Иван\","
                        + "\"guestAvatar\":\"   \"}")
                .statusCode())
        .isEqualTo(400);
  }

  private HttpResponse<String> post(String path, String body) throws Exception {
    return send("POST", path, body);
  }

  private HttpResponse<String> put(String path, String body) throws Exception {
    return send("PUT", path, body);
  }

  private HttpResponse<String> delete(String path) throws Exception {
    return client.send(
        HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(5)).DELETE().build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> send(String method, String path, String body) throws Exception {
    return client.send(
        HttpRequest.newBuilder(uri(path))
            .timeout(Duration.ofSeconds(5))
            .header("Content-Type", "application/json; charset=utf-8")
            .method(method, HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private URI uri(String path) {
    return URI.create("http://localhost:" + environment.getProperty("local.server.port") + path);
  }

  private String extractId(String body) throws Exception {
    return objectMapper.readTree(body).get("id").asText();
  }
}
