package ru.example.helpdesk.model;

import java.time.LocalDateTime;

// Карточка заявки из JOIN-запроса с именами вместо одних только внешних ключей.
public record TicketDetails(long id, String title, TicketStatus status, TicketPriority priority,
                            String customerName, String assigneeName, String categoryName,
                            LocalDateTime createdAt) {
}
