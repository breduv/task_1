package ru.example.helpdesk.model;

/** Категория заявки; record автоматически создаёт конструктор и методы доступа к полям. */
public record Category(long id, String name, String description, TicketPriority defaultPriority, boolean active) {
}
