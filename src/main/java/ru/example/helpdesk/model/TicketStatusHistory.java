package ru.example.helpdesk.model;

import java.time.LocalDateTime;

// Одна запись в истории статусов заявки
public record TicketStatusHistory(long id, long ticketId, TicketStatus oldStatus,
                                  TicketStatus newStatus, Long changedById, LocalDateTime changedAt) {
}
