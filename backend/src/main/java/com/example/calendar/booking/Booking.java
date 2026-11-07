package com.example.calendar.booking;

import java.time.LocalDate;
import java.time.LocalTime;

/** Запись гостя на встречу. Дата и время хранятся в часовом поясе владельца. */
public record Booking(
    String id,
    String meetingTypeId,
    String meetingTypeName,
    LocalDate date,
    LocalTime startTime,
    LocalTime endTime,
    String guestName,
    String guestAvatar,
    String note) {}
