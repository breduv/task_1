package helpdesk.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Ticket {
    private long id;
    private String title;
    private String description;
    private TicketStatus status;
    private final LocalDateTime createdAt;

    public Ticket(long id, String title, String description) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                "Название заявки не может быть пустым"
            );
        }

        this.id = id;
        this.title = title.trim();
        this.description = description;
        this.status = TicketStatus.NEW;
        this.createdAt = LocalDateTime.now();
    }

    public long getId() {
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

    public void startProcessing() {
        if (status != TicketStatus.NEW) {
            System.out.println(
                 "Ошибка: в работу можно взять только новую заявку"
            );
            return;
        }

        status = TicketStatus.IN_PROGRESS;
    }

    public void resolve() {
        if (status != TicketStatus.IN_PROGRESS) {
            System.out.println(
                "Ошибка: решить можно только заявку в работе"
            );
            return;
        }

        status = TicketStatus.RESOLVED;
    }

    public void close() {
        if (status != TicketStatus.RESOLVED) {
            System.out.println(
                "Ошибка: закрыть можно только решённую заявку"
            );
            return;
        }

        status = TicketStatus.CLOSED;
    }

    public void cancel() {
        if (status == TicketStatus.CLOSED) {
            System.out.println(
                "Ошибка: закрытую заявку отменить нельзя"
            );
            return;
        }

        status = TicketStatus.CANCELLED;
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        
        return "#" + id + " " + title + " | " + status + " | создана: " + createdAt.format(formatter);
    }
}