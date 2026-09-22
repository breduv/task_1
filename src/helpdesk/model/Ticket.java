package helpdesk.model;

public class Ticket {
    private long id;
    private String title;
    private String description;
    private TicketStatus status;

    public Ticket(long id, String title, String description) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = TicketStatus.NEW;
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
}