package com.example.calendar.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Отдаёт собранный фронтенд для клиентских маршрутов React Router. Статика лежит в {@code
 * src/main/resources/static} и попадает в jar при сборке Docker-образа.
 */
@Controller
public class SpaController {

  @GetMapping({"/", "/booking", "/booking/{typeId}", "/admin"})
  public String index() {
    return "forward:/index.html";
  }
}
