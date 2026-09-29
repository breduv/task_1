package ru.example.helpdesk.model;

import java.time.LocalDateTime;

// Подразделение поддержки; record автоматически даёт доступ к его полям.
public record Department(long id, String name, String description, LocalDateTime createdAt) {
}
