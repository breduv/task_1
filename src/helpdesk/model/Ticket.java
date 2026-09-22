package helpdesk.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Ticket {
    private long id;
    private String title;
    private String description;
    private TicketStatus status;
    private final LocalDateTime createdAt;
    private final TicketPriority priority;
    private final LocalDateTime deadline;
    private final List<StatusHistoryEntry> statusHistory = new ArrayList<>();

    public Ticket(long id, String title, String description, TicketPriority priority) {

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                 "Название заявки не может быть пустым"
            );
        }

        this.id = id;
        this.title = title.trim();
        this.description = description;
        this.priority = priority;
        this.status = TicketStatus.NEW;
        this.createdAt = LocalDateTime.now();
        this.deadline = calculateDeadline();
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

        changeStatus(TicketStatus.IN_PROGRESS);
    }

    public void resolve() {
        if (status != TicketStatus.IN_PROGRESS) {
            System.out.println(
                "Ошибка: решить можно только заявку в работе"
            );
            return;
        }

        changeStatus(TicketStatus.RESOLVED);
    }

    public void close() {
        if (status != TicketStatus.RESOLVED) {
            System.out.println(
                "Ошибка: закрыть можно только решённую заявку"
            );
            return;
        }

        changeStatus(TicketStatus.CLOSED);
    }

    public void cancel() {
        if (status == TicketStatus.CLOSED) {
            System.out.println(
                "Ошибка: закрытую заявку отменить нельзя"
            );
            return;
        }

        changeStatus(TicketStatus.CANCELLED);
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        return "#" + id + " " + title + " | приоритет: " + priority + " | статус: " + status + " | создана: " + createdAt.format(formatter) + " | срок: " + deadline.format(formatter);
    }

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

    private void changeStatus(TicketStatus newStatus) {
        StatusHistoryEntry entry = new StatusHistoryEntry(status, newStatus);

        statusHistory.add(entry);
        status = newStatus;
    }

    public List<StatusHistoryEntry> getStatusHistory() {
        return List.copyOf(statusHistory);
    }
}