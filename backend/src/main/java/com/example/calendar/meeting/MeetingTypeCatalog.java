package com.example.calendar.meeting;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Repository;

/** Каталог типов встреч. Хранится в памяти процесса и сбрасывается при перезапуске. */
@Repository
public class MeetingTypeCatalog {

  private final List<MeetingType> meetingTypes = new CopyOnWriteArrayList<>();

  public MeetingTypeCatalog() {
    meetingTypes.add(
        new MeetingType(
            "meeting-15", "Встреча 15 минут", "Короткий тип события для быстрого слота", 15));
    meetingTypes.add(
        new MeetingType(
            "meeting-30", "Встреча 30 минут", "Базовый тип события для бронирования", 30));
  }

  public List<MeetingType> findAll() {
    return List.copyOf(meetingTypes);
  }
}
