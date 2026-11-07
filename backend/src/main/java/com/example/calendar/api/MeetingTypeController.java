package com.example.calendar.api;

import com.example.calendar.meeting.MeetingType;
import com.example.calendar.meeting.MeetingTypeCatalog;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MeetingTypeController {

  private final MeetingTypeCatalog catalog;

  public MeetingTypeController(MeetingTypeCatalog catalog) {
    this.catalog = catalog;
  }

  @GetMapping("/meeting-types")
  public List<MeetingType> list() {
    return catalog.findAll();
  }

  @PostMapping("/meeting-types")
  public MeetingType create(@RequestBody MeetingTypeRequest request) {
    validate(request);
    return catalog.create(request.name(), request.description(), request.durationMinutes());
  }

  @PutMapping("/meeting-types/{id}")
  public MeetingType update(@PathVariable String id, @RequestBody MeetingTypeRequest request) {
    validate(request);
    return catalog.update(id, request.name(), request.description(), request.durationMinutes());
  }

  @DeleteMapping("/meeting-types/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable String id) {
    catalog.delete(id);
  }

  private void validate(MeetingTypeRequest request) {
    if (request.name() == null || request.name().isBlank()) {
      throw new IllegalArgumentException("Название типа встречи не должно быть пустым");
    }
    if (request.description() == null || request.description().isBlank()) {
      throw new IllegalArgumentException("Описание типа встречи не должно быть пустым");
    }
    if (request.durationMinutes() < 5 || request.durationMinutes() > 240) {
      throw new IllegalArgumentException("Длительность встречи должна быть от 5 до 240 минут");
    }
  }
}
