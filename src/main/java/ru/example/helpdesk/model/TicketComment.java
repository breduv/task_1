package ru.example.helpdesk.model;

import java.time.LocalDateTime;

// Комментарий к заявке и данные его автора
public class TicketComment {
    private Long id;
    private long ticketId;
    private long authorId;
    private String text;
    private boolean internal;
    private LocalDateTime createdAt;
    private String authorName;
    private UserRole authorRole;

    // Создаёт комментарий, internal=true означает внутреннюю заметку
    public TicketComment(long ticketId, long authorId, String text, boolean internal) {
        this.ticketId = ticketId;
        this.authorId = authorId;
        this.text = text;
        this.internal = internal;
    }

    // Через эти методы читаем и заполняем данные комментария
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public long getTicketId() { return ticketId; }
    public long getAuthorId() { return authorId; }
    public String getText() { return text; }
    public boolean isInternal() { return internal; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public UserRole getAuthorRole() { return authorRole; }
    public void setAuthorRole(UserRole authorRole) { this.authorRole = authorRole; }
}
