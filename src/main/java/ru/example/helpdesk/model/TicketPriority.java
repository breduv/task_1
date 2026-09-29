package ru.example.helpdesk.model;

/** Приоритеты заявки; имена обязаны совпадать с PostgreSQL ENUM ticket_priority. */
public enum TicketPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;
}
