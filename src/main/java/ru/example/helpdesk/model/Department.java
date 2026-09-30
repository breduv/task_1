package ru.example.helpdesk.model;

import java.time.LocalDateTime;

// Данные подразделения поддержки
public record Department(long id, String name, String description, LocalDateTime createdAt) {
}
