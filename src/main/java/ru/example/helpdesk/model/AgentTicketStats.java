package ru.example.helpdesk.model;

// Количество заявок сотрудника по основным статусам
public record AgentTicketStats(String agentName, long active, long resolved, long closed) {
}
