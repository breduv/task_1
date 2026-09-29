package ru.example.helpdesk.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Данные одной заявки; после чтения из БД поля заполняет JdbcTicketRepository. */
public class Ticket {
    private Long id;
    private String title;
    private String description;
    private TicketStatus status;
    private TicketPriority priority;
    private Long customerId;
    private Long assigneeId;
    private Long categoryId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closedAt;
    private LocalDateTime deadline;

    /** Пустой конструктор нужен, чтобы по очереди заполнить поля из ResultSet. */
    public Ticket() {
    }

    // Геттеры возвращают поля заявки, сеттеры заполняют их после INSERT или SELECT.
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
    /** При смене приоритета пересчитывает срок, если дата создания уже известна. */
    public void setPriority(TicketPriority priority) {
        this.priority = priority;
        if (createdAt != null && priority != null) this.deadline = calculateDeadline();
    }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    /** После получения даты из БД рассчитывает срок с учётом приоритета. */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        if (createdAt != null && priority != null) this.deadline = calculateDeadline();
    }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    /** Формирует короткую строку заявки для консольного вывода. */
    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        return "#" + id + " " + title + " | приоритет: " + priority + " | статус: " + status
                + " | создана: " + (createdAt == null ? "—" : createdAt.format(formatter))
                + " | срок: " + (deadline == null ? "—" : deadline.format(formatter));
    }

    /** Вычисляет срок из даты создания по правилам приоритетов первой работы. */
    private LocalDateTime calculateDeadline() {
        return switch (priority) {
            case CRITICAL -> createdAt.plusMinutes(30);
            case HIGH -> createdAt.plusHours(2);
            case MEDIUM -> createdAt.plusHours(8);
            case LOW -> createdAt.plusHours(24);
        };
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

}
