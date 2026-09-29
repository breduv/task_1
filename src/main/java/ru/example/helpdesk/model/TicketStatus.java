package ru.example.helpdesk.model;

// Статусы заявки; имена обязаны совпадать с PostgreSQL ENUM ticket_status.
public enum TicketStatus {
    NEW,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    CANCELLED
}
