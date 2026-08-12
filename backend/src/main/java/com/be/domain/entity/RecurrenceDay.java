package com.be.domain.entity;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * One weekday of a Course-linked Group's recurrence rule (LR-081,
 * LR-ADR-023) — each selected day carries its own start time and
 * duration, not one shared time for every day (customer's explicit
 * spec, 2026-08-12). Stored as JSON via RecurrenceDaysConverter, not a
 * child entity — always read/written as a whole small list, no need to
 * query by individual day.
 */
public record RecurrenceDay(DayOfWeek dayOfWeek, LocalTime startTime, int durationMinutes) {
}
