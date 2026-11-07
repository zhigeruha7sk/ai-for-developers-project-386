package com.example.calendar.api;

import com.example.calendar.booking.SlotUnavailableException;
import com.example.calendar.meeting.MeetingTypeNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Преобразует доменные ошибки в правильные HTTP-статусы. */
@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(SlotUnavailableException.class)
  public ResponseEntity<ProblemDetail> handleConflict(SlotUnavailableException exception) {
    return error(HttpStatus.CONFLICT, exception.getMessage());
  }

  @ExceptionHandler(MeetingTypeNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleNotFound(MeetingTypeNotFoundException exception) {
    return error(HttpStatus.NOT_FOUND, "Тип встречи не найден");
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ProblemDetail> handleBadRequest(IllegalArgumentException exception) {
    return error(HttpStatus.BAD_REQUEST, exception.getMessage());
  }

  private ResponseEntity<ProblemDetail> error(HttpStatus status, String detail) {
    return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
  }
}
