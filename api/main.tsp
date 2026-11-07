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

/** Данные для создания или изменения типа встречи. */
model MeetingTypeInput {
  /** Название типа встречи. */
  name: string;

  /** Описание типа встречи. */
  description: string;

  /** Длительность встречи в минутах. */
  durationMinutes: int32;
}

/** Свободный или занятый слот на выбранную дату. */
model Slot {
  /** Время начала слота. */
  startTime: plainTime;

  /** Время окончания слота. */
  endTime: plainTime;

  /** Можно ли записаться на этот слот. */
  available: boolean;
}

/** Данные для создания брони. */
model BookingInput {
  /** Идентификатор выбранного типа встречи. */
  meetingTypeId: string;

  /** Дата встречи. */
  date: plainDate;

  /** Время начала встречи. */
  startTime: plainTime;

  /** Имя гостя. */
  guestName: string;

  /** Аватар гостя. */
  guestAvatar: string;

  /** Необязательная заметка к брони. */
  note?: string;
}

/** Запись гостя на встречу. */
model Booking {
  /** Идентификатор брони. */
  id: string;

  /** Идентификатор типа встречи. */
  meetingTypeId: string;

  /** Название типа встречи на момент записи. */
  meetingTypeName: string;

  /** Дата встречи. */
  date: plainDate;

  /** Время начала встречи. */
  startTime: plainTime;

  /** Время окончания встречи. */
  endTime: plainTime;

  /** Имя гостя. */
  guestName: string;

  /** Аватар гостя. */
  guestAvatar: string;

  /** Необязательная заметка к брони. */
  note?: string;
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

  @summary("Создать тип встречи")
  @post
  @route("/meeting-types")
  op createMeetingType(@body body: MeetingTypeInput): MeetingType;

  @summary("Изменить тип встречи")
  @put
  @route("/meeting-types/{id}")
  op updateMeetingType(
    @path id: string,
    @body body: MeetingTypeInput,
  ): MeetingType;

  @summary("Удалить тип встречи")
  @delete
  @route("/meeting-types/{id}")
  op deleteMeetingType(@path id: string): void;

  @summary("Слоты типа встречи на выбранную дату")
  @get
  @route("/slots")
  op listSlots(@query meetingTypeId: string, @query date: plainDate): Slot[];

  @summary("Предстоящие записи")
  @get
  @route("/bookings")
  op listBookings(): Booking[];

  @summary("Записаться на встречу")
  @post
  @route("/bookings")
  op createBooking(@body body: BookingInput): Booking;
}
