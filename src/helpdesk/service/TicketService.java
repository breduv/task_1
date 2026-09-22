package helpdesk.service;

import helpdesk.model.Ticket;

public class TicketService {
    private final NotificationService notificationService;

    public TicketService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void startTicket(Ticket ticket) {
        ticket.startProcessing();

        notificationService.send(
                "Заявка №" + ticket.getId() + " принята в работу"
        );
    }

    public void resolveTicket(Ticket ticket) {
        ticket.resolve();

        notificationService.send(
                "По заявке №" + ticket.getId() + " найдено решение"
        );
    }

    public void closeTicket(Ticket ticket) {
        ticket.close();

        notificationService.send(
                "Заявка №" + ticket.getId() + " закрыта"
        );
    }
}