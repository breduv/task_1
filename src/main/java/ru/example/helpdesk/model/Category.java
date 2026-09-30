package ru.example.helpdesk.model;

// Данные категории заявки
public record Category(long id, String name, String description, TicketPriority defaultPriority, boolean active) {
}
