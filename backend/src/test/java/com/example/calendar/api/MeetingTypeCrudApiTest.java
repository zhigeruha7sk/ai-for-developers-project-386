package com.example.calendar.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MeetingTypeCrudApiTest {

  @Autowired private Environment environment;

  private final HttpClient client =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  @Test
  void ownerCreatesUpdatesAndDeletesMeetingType() throws Exception {
    String base = "http://localhost:" + environment.getProperty("local.server.port");

    HttpResponse<String> created =
        send(
            "POST",
            base + "/api/meeting-types",
            "{\"name\":\"Встреча 90 минут\",\"description\":\"Длинная тестовая встреча\","
                + "\"durationMinutes\":90}");
    assertThat(created.statusCode()).isEqualTo(200);
    assertThat(created.body())
        .contains("\"name\":\"Встреча 90 минут\"")
        .contains("\"durationMinutes\":90");

    String id = extractId(created.body());
    assertThat(id).isNotBlank();

    HttpResponse<String> updated =
        send(
            "PUT",
            base + "/api/meeting-types/" + id,
            "{\"name\":\"Встреча 60 минут\",\"description\":\"Обновлённая встреча\","
                + "\"durationMinutes\":60}");
    assertThat(updated.statusCode()).isEqualTo(200);
    assertThat(updated.body())
        .contains("\"name\":\"Встреча 60 минут\"")
        .contains("\"durationMinutes\":60");

    HttpResponse<String> deleted = send("DELETE", base + "/api/meeting-types/" + id, null);
    assertThat(deleted.statusCode()).isEqualTo(204);

    HttpResponse<String> list =
        client.send(
            HttpRequest.newBuilder(URI.create(base + "/api/meeting-types")).GET().build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(list.body()).doesNotContain("Встреча 60 минут");
  }

  @Test
  void rejectsMeetingTypeWithEmptyName() throws Exception {
    String base = "http://localhost:" + environment.getProperty("local.server.port");
    HttpResponse<String> response =
        send(
            "POST",
            base + "/api/meeting-types",
            "{\"name\":\"\",\"description\":\"x\",\"durationMinutes\":30}");
    assertThat(response.statusCode()).isEqualTo(400);
  }

  private HttpResponse<String> send(String method, String url, String body) throws Exception {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(5));
    if (body == null) {
      builder.method(method, HttpRequest.BodyPublishers.noBody());
    } else {
      builder
          .method(method, HttpRequest.BodyPublishers.ofString(body))
          .header("Content-Type", "application/json; charset=utf-8");
    }
    return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
  }

  private String extractId(String body) {
    int start = body.indexOf("\"id\":\"") + "\"id\":\"".length();
    int end = body.indexOf('"', start);
    return start >= "\"id\":\"".length() ? body.substring(start, end) : "";
  }
}
