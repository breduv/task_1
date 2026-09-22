package helpdesk.repository;

import helpdesk.model.Ticket;

import java.util.ArrayList;
import java.util.List;

public class TicketRepository {
    private final List<Ticket> tickets = new ArrayList<>();

    public void add(Ticket ticket) {
        tickets.add(ticket);
    }

    public List<Ticket> findAll() {
        return new ArrayList<>(tickets);
    }
}