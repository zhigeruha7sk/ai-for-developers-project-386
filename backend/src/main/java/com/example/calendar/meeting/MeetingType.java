package com.example.calendar.meeting;

/** Тип встречи, который заводит владелец календаря. */
public record MeetingType(String id, String name, String description, int durationMinutes) {}
