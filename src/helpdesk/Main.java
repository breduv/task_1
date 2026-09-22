package helpdesk;

import helpdesk.model.Administrator;
import helpdesk.model.Customer;
import helpdesk.model.SupportAgent;
import helpdesk.model.Ticket;
import helpdesk.model.User;
import helpdesk.repository.TicketRepository;
import helpdesk.service.ConsoleNotificationService;
import helpdesk.service.NotificationService;
import helpdesk.service.TicketService;

import java.util.ArrayList;
import java.util.List;

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
                "Компьютер не подключается к беспроводной сети"
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
                "Принтер не отвечает"
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
                "Описание заявки"
            );

            System.out.println(
                "Создана заявка: " + invalidTicket.getTitle()
            );
        } catch (IllegalArgumentException exception) {
            System.out.println(
                "Ошибка: " + exception.getMessage()
            );
        }


        TicketRepository ticketRepository = new TicketRepository();

        ticketRepository.add(ticket);
        ticketRepository.add(cancelledTicket);

        System.out.println("\nПолный список заявок:");

        for (Ticket savedTicket : ticketRepository.findAll()) {
            System.out.println(
                savedTicket.toString()
            );
        }
    }
}