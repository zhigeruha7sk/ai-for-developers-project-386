package com.example.calendar;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CalendarApplication {

  public static void main(String[] args) {
    SpringApplication.run(CalendarApplication.class, args);
  }

  @Bean
  Clock clock() {
    return Clock.system(CalendarTime.ZONE);
  }
}
