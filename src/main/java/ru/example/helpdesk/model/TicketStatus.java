package ru.example.helpdesk.model;

// Статусы заявки, такие же как в PostgreSQL
public enum TicketStatus {
    NEW,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    CANCELLED
}
