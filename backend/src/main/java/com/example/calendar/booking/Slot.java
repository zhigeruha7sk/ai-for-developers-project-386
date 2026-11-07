package com.example.calendar.booking;

import java.time.LocalTime;

/** Свободный или занятый слот на выбранную дату. */
public record Slot(LocalTime startTime, LocalTime endTime, boolean available) {}
