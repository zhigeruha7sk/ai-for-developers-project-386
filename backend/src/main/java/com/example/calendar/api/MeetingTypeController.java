package com.example.calendar.api;

import com.example.calendar.meeting.MeetingType;
import com.example.calendar.meeting.MeetingTypeCatalog;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
