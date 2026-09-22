package helpdesk.repository;

import helpdesk.model.Ticket;
import helpdesk.model.TicketStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TicketRepository {
    private final List<Ticket> tickets = new ArrayList<>();

    public void add(Ticket ticket) {
        tickets.add(ticket);
    }

    public List<Ticket> findAll() {
        return new ArrayList<>(tickets);
    }

    public List<Ticket> findByStatus(TicketStatus status) {
        List<Ticket> result = new ArrayList<>();

        for (Ticket ticket : tickets) {
            if (ticket.getStatus() == status) {
                result.add(ticket);
            }
        }

        return result;
    }

    public Optional<Ticket> findById(long id) {
        for (Ticket ticket : tickets) {
            if (ticket.getId() == id) {
                return Optional.of(ticket);
            }
        }

        return Optional.empty();
    }

    public Map<TicketStatus, Integer> getStatistics() {
        Map<TicketStatus, Integer> statistics = new EnumMap<>(TicketStatus.class);

        for (TicketStatus status : TicketStatus.values()) {
            statistics.put(status, 0);
        }

        for (Ticket ticket : tickets) {
            TicketStatus status = ticket.getStatus();

            statistics.put(
                status,
                statistics.get(status) + 1
            );
        }

        return statistics;
    }

    public List<Ticket> findOverdue() {
        List<Ticket> overdueTickets = new ArrayList<>();
        LocalDateTime currentTime = LocalDateTime.now();

        for (Ticket ticket : tickets) {
            boolean deadlineExpired = currentTime.isAfter(ticket.getDeadline());

            boolean ticketNotFinished = ticket.getStatus() != TicketStatus.RESOLVED && ticket.getStatus() != TicketStatus.CLOSED;

            if (deadlineExpired && ticketNotFinished) {
                overdueTickets.add(ticket);
            }
        }

        return overdueTickets;
    }
}