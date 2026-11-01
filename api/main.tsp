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

/** Тип встречи, который заводит владелец календаря. */
model MeetingType {
  /** Устойчивый идентификатор типа встречи. */
  id: string;

  /** Название типа встречи. */
  name: string;

  /** Описание типа встречи. */
  description: string;

  /** Длительность встречи в минутах. */
  durationMinutes: int32;
}

@route("/api")
namespace Api {
  @summary("Проверка работоспособности сервиса")
  @get
  @route("/health")
  op health(): HealthResponse;

  @summary("Список типов встреч")
  @get
  @route("/meeting-types")
  op listMeetingTypes(): MeetingType[];
}
