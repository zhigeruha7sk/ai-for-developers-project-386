package com.example.calendar.meeting;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Repository;

/**
 * Каталог типов встреч. Хранится в памяти процесса и сбрасывается при перезапуске. Владелец заводит
 * и редактирует типы в админке.
 */
@Repository
public class MeetingTypeCatalog {

  private final CopyOnWriteArrayList<MeetingType> meetingTypes = new CopyOnWriteArrayList<>();

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

  public Optional<MeetingType> findById(String id) {
    return meetingTypes.stream().filter(m -> m.id().equals(id)).findFirst();
  }

  public synchronized MeetingType create(String name, String description, int durationMinutes) {
    String id = UUID.randomUUID().toString();
    MeetingType type = new MeetingType(id, name, description, durationMinutes);
    meetingTypes.add(type);
    return type;
  }

  public synchronized MeetingType update(
      String id, String name, String description, int durationMinutes) {
    if (!exists(id)) {
      throw new MeetingTypeNotFoundException(id);
    }
    MeetingType updated = new MeetingType(id, name, description, durationMinutes);
    meetingTypes.replaceAll(m -> m.id().equals(id) ? updated : m);
    return updated;
  }

  public synchronized void delete(String id) {
    meetingTypes.removeIf(m -> m.id().equals(id));
  }

  private boolean exists(String id) {
    return meetingTypes.stream().anyMatch(m -> m.id().equals(id));
  }
}
