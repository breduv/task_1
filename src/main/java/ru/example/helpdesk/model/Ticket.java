package ru.example.helpdesk.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Данные заявки, которую создаём или читаем из базы
public class Ticket {
    private Long id;
    private String title;
    private String description;
    private TicketStatus status;
    private TicketPriority priority;
    private Long customerId;
    private Long assigneeId;
    private Long categoryId;
    private Long knowledgeArticleId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closedAt;
    private LocalDateTime dueAt;

    // Создаёт пустую заявку перед заполнением данными из базы
    public Ticket() {
    }

    // Через эти методы читаем и заполняем поля заявки
    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setStatus(TicketStatus status) { this.status = status; }
    public void setPriority(TicketPriority priority) { this.priority = priority; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getKnowledgeArticleId() { return knowledgeArticleId; }
    public void setKnowledgeArticleId(Long knowledgeArticleId) { this.knowledgeArticleId = knowledgeArticleId; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    public LocalDateTime getDueAt() { return dueAt; }
    public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }

    // Собирает короткое описание заявки для консоли
    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        return "#" + id + " " + title + " | приоритет: " + priority + " | статус: " + status
                + " | создана: " + (createdAt == null ? "—" : createdAt.format(formatter))
                + " | срок: " + (dueAt == null ? "—" : dueAt.format(formatter));
    }

    public TicketPriority getPriority() {
        return priority;
    }

}
