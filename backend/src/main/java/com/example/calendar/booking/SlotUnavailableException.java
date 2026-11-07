package com.example.calendar.booking;

/** Слот уже занят другой встречей. */
public class SlotUnavailableException extends RuntimeException {

  public SlotUnavailableException() {
    super("Слот уже занят");
  }
}
