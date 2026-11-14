package com.example.calendar.api;

import static com.example.calendar.CalendarTime.ZONE;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Проверяет, что HTTP-ответы совпадают с OpenAPI-контрактом: пути, методы, поля, успешные ответы и
 * ошибки данных/конфликта в формате RFC 7807.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiContractTest {

  @Autowired private Environment environment;

  private final HttpClient client =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void healthMatchesContract() throws Exception {
    HttpResponse<String> response = get("/api/health");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.headers().firstValue("Content-Type"))
        .hasValueSatisfying(value -> assertThat(value).startsWith("application/json"));
    assertThat(objectMapper.readTree(response.body()).get("status").asText()).isEqualTo("UP");
  }

  @Test
  void meetingTypeCrudMatchesContract() throws Exception {
    HttpResponse<String> list = get("/api/meeting-types");
    assertThat(list.statusCode()).isEqualTo(200);
    JsonNode seeded = objectMapper.readTree(list.body());
    assertThat(seeded.isArray()).isTrue();
    assertThat(ids(seeded)).contains("meeting-15", "meeting-30");

    HttpResponse<String> created =
        post(
            "/api/meeting-types",
            "{\"name\":\"Контрактная встреча\",\"description\":\"Проверка контракта\","
                + "\"durationMinutes\":60}");
    assertThat(created.statusCode()).isEqualTo(200);
    JsonNode createdBody = objectMapper.readTree(created.body());
    String id = createdBody.get("id").asText();
    assertThat(id).isNotBlank();
    assertThat(createdBody.get("name").asText()).isEqualTo("Контрактная встреча");
    assertThat(createdBody.get("description").asText()).isEqualTo("Проверка контракта");
    assertThat(createdBody.get("durationMinutes").asInt()).isEqualTo(60);

    HttpResponse<String> updated =
        put(
            "/api/meeting-types/" + id,
            "{\"name\":\"Контрактная встреча 45\",\"description\":\"Обновлено\","
                + "\"durationMinutes\":45}");
    assertThat(updated.statusCode()).isEqualTo(200);
    JsonNode updatedBody = objectMapper.readTree(updated.body());
    assertThat(updatedBody.get("id").asText()).isEqualTo(id);
    assertThat(updatedBody.get("name").asText()).isEqualTo("Контрактная встреча 45");
    assertThat(updatedBody.get("durationMinutes").asInt()).isEqualTo(45);

    HttpResponse<String> deleted = delete("/api/meeting-types/" + id);
    assertThat(deleted.statusCode()).isEqualTo(204);
    assertThat(deleted.body()).isEmpty();
  }

  @Test
  void slotsMatchContract() throws Exception {
    LocalDate date = LocalDate.now(ZONE).plusDays(11);

    HttpResponse<String> response = get("/api/slots?meetingTypeId=meeting-30&date=" + date);

    assertThat(response.statusCode()).isEqualTo(200);
    JsonNode slots = objectMapper.readTree(response.body());
    assertThat(slots.isArray()).isTrue();
    assertThat(slots.size()).isEqualTo(16);
    JsonNode first = slots.get(0);
    assertThat(first.get("startTime").asText()).isEqualTo("09:00:00");
    assertThat(first.get("endTime").asText()).isEqualTo("09:30:00");
    assertThat(first.get("available").asBoolean()).isTrue();
  }

  @Test
  void bookingsMatchContract() throws Exception {
    LocalDate date = LocalDate.now(ZONE).plusDays(10);
    HttpResponse<String> created =
        post(
            "/api/bookings",
            "{\"meetingTypeId\":\"meeting-15\",\"date\":\""
                + date
                + "\",\"startTime\":\"09:00\",\"guestName\":\"Контрактный гость\","
                + "\"guestAvatar\":\"🦊\",\"note\":\"Позвонить заранее\"}");

    assertThat(created.statusCode()).isEqualTo(200);
    JsonNode booking = objectMapper.readTree(created.body());
    assertThat(booking.get("id").asText()).isNotBlank();
    assertThat(booking.get("meetingTypeId").asText()).isEqualTo("meeting-15");
    assertThat(booking.get("meetingTypeName").asText()).isEqualTo("Встреча 15 минут");
    assertThat(booking.get("date").asText()).isEqualTo(date.toString());
    assertThat(booking.get("startTime").asText()).isEqualTo("09:00:00");
    assertThat(booking.get("endTime").asText()).isEqualTo("09:15:00");
    assertThat(booking.get("guestName").asText()).isEqualTo("Контрактный гость");
    assertThat(booking.get("guestAvatar").asText()).isEqualTo("🦊");
    assertThat(booking.get("note").asText()).isEqualTo("Позвонить заранее");

    HttpResponse<String> upcoming = get("/api/bookings");
    assertThat(upcoming.statusCode()).isEqualTo(200);
    JsonNode list = objectMapper.readTree(upcoming.body());
    assertThat(list.isArray()).isTrue();
    assertThat(ids(list)).contains(booking.get("id").asText());
  }

  @Test
  void dataErrorsMatchContract() throws Exception {
    LocalDate date = LocalDate.now(ZONE).plusDays(12);

    assertProblemDetail(
        post(
            "/api/bookings",
            "{\"meetingTypeId\":\"meeting-30\",\"startTime\":\"09:00\","
                + "\"guestName\":\"Гость\",\"guestAvatar\":\"🦊\"}"),
        400,
        "Дата встречи не должна быть пустой");

    assertProblemDetail(
        post(
            "/api/meeting-types",
            "{\"name\":\"Слишком короткая\",\"description\":\"Проверка\","
                + "\"durationMinutes\":4}"),
        400,
        "Длительность встречи должна быть от 5 до 240 минут");

    assertProblemDetail(
        get("/api/slots?meetingTypeId=meeting-30&date=not-a-date"),
        400,
        "Запрос не соответствует контракту API");

    assertProblemDetail(
        post(
            "/api/bookings",
            "{\"meetingTypeId\":\"meeting-30\",\"date\":\""
                + date
                + "\",\"startTime\":\"09:00\",\"guestName\":\"  \",\"guestAvatar\":\"🦊\"}"),
        400,
        "Имя гостя не должно быть пустым");
  }

  @Test
  void notFoundErrorsMatchContract() throws Exception {
    assertProblemDetail(
        post(
            "/api/bookings",
            "{\"meetingTypeId\":\"meeting-unknown\",\"date\":\""
                + LocalDate.now(ZONE).plusDays(12)
                + "\",\"startTime\":\"09:00\",\"guestName\":\"Гость\","
                + "\"guestAvatar\":\"🦊\"}"),
        404,
        "Тип встречи не найден");

    assertProblemDetail(
        get("/api/slots?meetingTypeId=meeting-unknown&date=" + LocalDate.now(ZONE).plusDays(12)),
        404,
        "Тип встречи не найден");
  }

  @Test
  void conflictErrorMatchesContract() throws Exception {
    LocalDate date = LocalDate.now(ZONE).plusDays(9);
    HttpResponse<String> createdType =
        post(
            "/api/meeting-types",
            "{\"name\":\"Контрактная встреча 60\",\"description\":\"Пересечение\","
                + "\"durationMinutes\":60}");
    assertThat(createdType.statusCode()).isEqualTo(200);
    String typeId = objectMapper.readTree(createdType.body()).get("id").asText();

    try {
      HttpResponse<String> longBooking =
          post(
              "/api/bookings",
              "{\"meetingTypeId\":\""
                  + typeId
                  + "\",\"date\":\""
                  + date
                  + "\",\"startTime\":\"10:00\",\"guestName\":\"Долгая встреча\","
                  + "\"guestAvatar\":\"🐯\"}");
      assertThat(longBooking.statusCode()).isEqualTo(200);

      // 10:00–11:00 и 10:30–11:00: частичное наложение встреч разной длительности.
      assertProblemDetail(
          post(
              "/api/bookings",
              "{\"meetingTypeId\":\"meeting-30\",\"date\":\""
                  + date
                  + "\",\"startTime\":\"10:30\",\"guestName\":\"Короткая встреча\","
                  + "\"guestAvatar\":\"🦁\"}"),
          409,
          "Слот уже занят");
    } finally {
      delete("/api/meeting-types/" + typeId);
    }
  }

  private List<String> ids(JsonNode array) {
    List<String> ids = new ArrayList<>();
    array.forEach(node -> ids.add(node.get("id").asText()));
    return ids;
  }

  private void assertProblemDetail(HttpResponse<String> response, int status, String detailFragment)
      throws Exception {
    assertThat(response.statusCode()).isEqualTo(status);
    assertThat(response.headers().firstValue("Content-Type"))
        .hasValueSatisfying(value -> assertThat(value).startsWith("application/json"));
    JsonNode body = objectMapper.readTree(response.body());
    assertThat(body.get("type").asText()).isNotBlank();
    assertThat(body.get("title").asText()).isNotBlank();
    assertThat(body.get("status").asInt()).isEqualTo(status);
    assertThat(body.get("detail").asText()).contains(detailFragment);
  }

  private HttpResponse<String> get(String path) throws Exception {
    return send("GET", path, null);
  }

  private HttpResponse<String> post(String path, String body) throws Exception {
    return send("POST", path, body);
  }

  private HttpResponse<String> put(String path, String body) throws Exception {
    return send("PUT", path, body);
  }

  private HttpResponse<String> delete(String path) throws Exception {
    return send("DELETE", path, null);
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
