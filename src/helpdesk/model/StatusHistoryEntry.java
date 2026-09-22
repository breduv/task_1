package helpdesk.model;

import java.time.LocalDateTime;

public class StatusHistoryEntry {
    private final TicketStatus oldStatus;
    private final TicketStatus newStatus;
    private final LocalDateTime changedAt;

    public StatusHistoryEntry(TicketStatus oldStatus, TicketStatus newStatus) {
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedAt = LocalDateTime.now();
    }

    public TicketStatus getOldStatus() {
        return oldStatus;
    }

    public TicketStatus getNewStatus() {
        return newStatus;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }
}