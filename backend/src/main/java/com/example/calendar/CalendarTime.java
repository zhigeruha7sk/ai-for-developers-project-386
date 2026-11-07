package com.example.calendar;

import java.time.LocalTime;
import java.time.ZoneId;

/** Доменные константы времени: часовой пояс и рабочие часы владельца. */
public final class CalendarTime {

  public static final ZoneId ZONE = ZoneId.of("Asia/Almaty");

  public static final LocalTime MORNING_START = LocalTime.of(9, 0);
  public static final LocalTime MORNING_END = LocalTime.of(13, 0);
  public static final LocalTime AFTERNOON_START = LocalTime.of(14, 0);
  public static final LocalTime AFTERNOON_END = LocalTime.of(18, 0);

  /** Окно записи — ближайшие 14 календарных дней, включая сегодняшний. */
  public static final int WINDOW_DAYS = 14;

  private CalendarTime() {}
}
