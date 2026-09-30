package ru.example.helpdesk.model;

// Сроки ответа и решения для приоритета заявки
public record SlaPolicy(long id, String name, TicketPriority priority,
                        int responseMinutes, int resolveMinutes) {
}
