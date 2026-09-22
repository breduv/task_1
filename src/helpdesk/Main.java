package helpdesk;

import helpdesk.model.Administrator;
import helpdesk.model.Customer;
import helpdesk.model.StatusHistoryEntry;
import helpdesk.model.SupportAgent;
import helpdesk.model.Ticket;
import helpdesk.model.TicketPriority;
import helpdesk.model.TicketStatus;
import helpdesk.model.User;
import helpdesk.repository.TicketRepository;
import helpdesk.service.ConsoleNotificationService;
import helpdesk.service.EmailNotificationService;
import helpdesk.service.NotificationService;
import helpdesk.service.TelegramNotificationService;
import helpdesk.service.TicketService;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== HELP DESK ===");

        Customer customer = new Customer(
                1,
                "Анна Петрова",
                "anna@mail.ru"
        );

        SupportAgent supportAgent = new SupportAgent(
                2,
                "Сергей Иванов",
                "sergey@helpdesk.ru"
        );

        Administrator administrator = new Administrator(
                3,
                "Олег Смирнов",
                "admin@helpdesk.ru"
        );

        List<User> users = new ArrayList<>();

        users.add(customer);
        users.add(supportAgent);
        users.add(administrator);

        System.out.println("\nПользователи:");

        for (User user : users) {
            user.performAction();
        }

        Ticket ticket = new Ticket(
                1,
                "Не работает Wi-Fi",
                "Компьютер не подключается к беспроводной сети",
                TicketPriority.HIGH
        );

        System.out.println("\nКлиент: " + customer.getName());

        System.out.println(
            "Заявка #" + ticket.getId() + ": " + ticket.getTitle() + " | " + ticket.getStatus()
        );

        NotificationService notificationService = new ConsoleNotificationService();

        TicketService ticketService = new TicketService(notificationService);

        ticketService.startTicket(ticket);
        System.out.println("Статус: " + ticket.getStatus());

        ticketService.resolveTicket(ticket);
        System.out.println("Статус: " + ticket.getStatus());

        ticketService.closeTicket(ticket);
        System.out.println("Статус: " + ticket.getStatus());


        System.out.println("\n=== ПРОВЕРКА ОТМЕНЫ ===");

        Ticket cancelledTicket = new Ticket(
                2,
                "Не работает принтер",
                "Принтер не отвечает",
                TicketPriority.MEDIUM
        );

        System.out.println(
                "Статус до отмены: " + cancelledTicket.getStatus()
        );

        cancelledTicket.cancel();

        System.out.println(
                "Статус после отмены: " + cancelledTicket.getStatus()
        );

        System.out.println("\nПопытка отменить закрытую заявку:");

        ticket.cancel();

        System.out.println(
                "Статус закрытой заявки: " + ticket.getStatus()
        );


        System.out.println("\n=== ПРОВЕРКА НАЗВАНИЯ ===");

        try {
            Ticket invalidTicket = new Ticket(
                3,
                "   ",
                "Описание заявки",
                TicketPriority.LOW
            );

            System.out.println(
                "Создана заявка: " + invalidTicket.getTitle()
            );
        } catch (IllegalArgumentException exception) {
            System.out.println(
                "Ошибка: " + exception.getMessage()
            );
        }


        System.out.println("\n=== ПРОВЕРКА УВЕДОМЛЕНИЙ ===");

        Ticket emailTicket = new Ticket(
            3,
            "Не работает электронная почта",
            "Письма не отправляются",
            TicketPriority.CRITICAL
        );

        NotificationService emailService = new EmailNotificationService();

        TicketService emailTicketService = new TicketService(emailService);

        emailTicketService.startTicket(emailTicket);

        Ticket telegramTicket = new Ticket(
            4,
            "Не работает принтер",
            "Принтер не отвечает",
            TicketPriority.LOW
        );

        NotificationService telegramService = new TelegramNotificationService();

        TicketService telegramTicketService = new TicketService(telegramService);

        telegramTicketService.startTicket(telegramTicket);


        TicketRepository ticketRepository = new TicketRepository();

        ticketRepository.add(ticket);
        ticketRepository.add(cancelledTicket);
        ticketRepository.add(emailTicket);
        ticketRepository.add(telegramTicket);


        System.out.println("\n=== ПОИСК ПО СТАТУСУ ===");

        for (Ticket foundTicket : ticketRepository.findByStatus(TicketStatus.IN_PROGRESS)) {
            System.out.println(foundTicket);
        }


        System.out.println("\n=== ПОИСК ПО ID ===");

        Optional<Ticket> foundTicket = ticketRepository.findById(2);

        if (foundTicket.isPresent()) {
            System.out.println(
                "Заявка найдена: " + foundTicket.get()
            );
        } else {
            System.out.println("Заявка не найдена");
        }


        System.out.println("\n=== СТАТИСТИКА ЗАЯВОК ===");

        for (Map.Entry<TicketStatus, Integer> entry : ticketRepository.getStatistics().entrySet()) {

            System.out.println(
                entry.getKey() + ": " + entry.getValue()
            );
        }


        System.out.println("\nПолный список заявок:");

        for (Ticket savedTicket : ticketRepository.findAll()) {
            System.out.println(
                savedTicket.toString()
            );
        }


        DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

        System.out.println("История статусов первой заявки:");

        for (StatusHistoryEntry entry : ticket.getStatusHistory()) {
            System.out.println(
                entry.getOldStatus() + " -> " + entry.getNewStatus() + " | " + entry.getChangedAt().format(formatter)
            );
        }

        System.out.println("\nПросроченные заявки:");

        List<Ticket> overdueTickets = ticketRepository.findOverdue();

        if (overdueTickets.isEmpty()) {
            System.out.println("Просроченных заявок нет");
        } else {
            for (Ticket overdueTicket : overdueTickets) {
                System.out.println(overdueTicket);
            }
        }
    }
}