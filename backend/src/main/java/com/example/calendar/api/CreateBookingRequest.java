package com.example.calendar.api;

import java.time.LocalDate;
import java.time.LocalTime;

/** Данные для создания брони. */
public record CreateBookingRequest(
    String meetingTypeId,
    LocalDate date,
    LocalTime startTime,
    String guestName,
    String guestAvatar,
    String note) {}
