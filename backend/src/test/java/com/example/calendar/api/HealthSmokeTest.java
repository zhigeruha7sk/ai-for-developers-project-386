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
class HealthSmokeTest {

  @Autowired private Environment environment;

  @Test
  void healthEndpointReportsUp() throws Exception {
    String port = environment.getProperty("local.server.port");
    HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/health"))
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.headers().firstValue("Content-Type"))
        .hasValueSatisfying(value -> assertThat(value).startsWith("application/json"));
    assertThat(response.body()).contains("\"status\":\"UP\"");
  }
}
