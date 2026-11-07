package com.example.calendar.api;

/** Данные для создания или изменения типа встречи. */
public record MeetingTypeRequest(String name, String description, int durationMinutes) {}
