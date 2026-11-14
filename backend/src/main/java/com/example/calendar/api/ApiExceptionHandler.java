package com.example.calendar.api;

import com.example.calendar.booking.SlotUnavailableException;
import com.example.calendar.meeting.MeetingTypeNotFoundException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class
  })
  public ResponseEntity<ProblemDetail> handleMalformedRequest(Exception exception) {
    return error(HttpStatus.BAD_REQUEST, "Запрос не соответствует контракту API");
  }

  private ResponseEntity<ProblemDetail> error(HttpStatus status, String detail) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setType(URI.create("about:blank"));
    return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(problem);
  }
}
