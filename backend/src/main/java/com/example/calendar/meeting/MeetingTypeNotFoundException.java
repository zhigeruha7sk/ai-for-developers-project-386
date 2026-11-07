package com.example.calendar.meeting;

/** Тип встречи с таким идентификатором не найден. */
public class MeetingTypeNotFoundException extends RuntimeException {

  public MeetingTypeNotFoundException(String id) {
    super("Meeting type not found: " + id);
  }
}
