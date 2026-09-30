package ru.example.helpdesk.model;

import java.time.LocalDateTime;

// Статья с описанием решения проблемы
public record KnowledgeArticle(long id, long categoryId, String title,
                               String content, LocalDateTime createdAt) {
}
