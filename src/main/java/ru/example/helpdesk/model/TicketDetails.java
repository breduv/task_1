package ru.example.helpdesk.model;

import java.time.LocalDateTime;

// Карточка заявки с именами клиента, исполнителя и категории
public record TicketDetails(long id, String title, TicketStatus status, TicketPriority priority,
                            String customerName, String assigneeName, String categoryName,
                            LocalDateTime createdAt) {
}
