import "@typespec/http";
import "@typespec/openapi3";

using Http;

@service(#{ title: "Calendar API" })
@server("http://localhost:8080", "Local development server")
namespace Calendar;

/** Состояние сервиса. */
enum HealthStatus {
  UP: "UP",
}

/** Ответ проверки работоспособности. */
model HealthResponse {
  status: HealthStatus;
}

@route("/api")
namespace Api {
  @summary("Проверка работоспособности сервиса")
  @get
  @route("/health")
  op health(): HealthResponse;
}
