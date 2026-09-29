package ru.example.helpdesk.model;

import java.time.LocalDateTime;

/** Одна сохранённая запись о переходе заявки между статусами. */
public record TicketStatusHistory(long id, long ticketId, TicketStatus oldStatus,
                                  TicketStatus newStatus, Long changedById, LocalDateTime changedAt) {
}
