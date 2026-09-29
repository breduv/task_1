package ru.example.helpdesk;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import ru.example.helpdesk.config.DatabaseConfig;
import ru.example.helpdesk.model.Category;
import ru.example.helpdesk.model.Ticket;
import ru.example.helpdesk.model.TicketComment;
import ru.example.helpdesk.model.TicketDetails;
import ru.example.helpdesk.model.TicketStatus;
import ru.example.helpdesk.model.User;
import ru.example.helpdesk.model.UserRole;
import ru.example.helpdesk.repository.jdbc.JdbcCatalogRepository;
import ru.example.helpdesk.repository.jdbc.JdbcCommentRepository;
import ru.example.helpdesk.repository.jdbc.JdbcReportRepository;
import ru.example.helpdesk.repository.jdbc.JdbcTicketRepository;
import ru.example.helpdesk.repository.jdbc.JdbcUserRepository;
import ru.example.helpdesk.service.ConsoleNotificationService;
import ru.example.helpdesk.service.TicketService;

public class Main {
    // Запускает обязательный сценарий: от подключения к БД до проверки отчётов.
    public static void main(String[] args) {
        JdbcTicketRepository tickets = new JdbcTicketRepository();
        JdbcUserRepository users = new JdbcUserRepository();
        JdbcCatalogRepository catalog = new JdbcCatalogRepository();
        JdbcCommentRepository comments = new JdbcCommentRepository();
        JdbcReportRepository reports = new JdbcReportRepository();
        TicketService service = new TicketService(tickets, users, catalog, new ConsoleNotificationService());

        try (Connection connection = DatabaseConfig.getConnection()) {
            System.out.println("Подключение к PostgreSQL успешно");
            System.out.println("AutoCommit = " + connection.getAutoCommit());
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось подключиться к PostgreSQL", e);
        }

        // Список из БД показывает, что заявки переживают перезапуск Java-программы.
        System.out.println("Заявки, сохранённые до запуска программы:");
        List<Ticket> savedTickets = tickets.findAll();
        if (savedTickets.isEmpty()) System.out.println("  Пока нет заявок");
        else savedTickets.forEach(ticket -> System.out.println("  " + ticket));

        // Пункт 36: полный сценарий от поиска клиента до повторного чтения заявки.
        User customer = users.findByEmail("anna@example.org")
                .orElseThrow(() -> new IllegalStateException("Нет тестового клиента anna@example.org"));
        if (customer.getRole() != UserRole.CUSTOMER)
            throw new IllegalStateException("Найденный пользователь не является клиентом");
        System.out.println("Клиент: " + customer.getName());

        List<Category> categories = catalog.findAllCategories();
        System.out.println("Категории:");
        categories.forEach(category -> System.out.println("  #" + category.id() + " " + category.name()));
        Category category = categories.stream().filter(Category::active).findFirst()
                .orElseThrow(() -> new IllegalStateException("Нет активной категории"));

        Ticket created = service.createTicket("Проверка JDBC Help Desk",
                "Тестовая заявка для обязательного сценария практической работы №2",
                category.defaultPriority(), customer.getId(), category.id());
        long ticketId = created.getId();
        System.out.println("Создана заявка PostgreSQL с id = " + ticketId);
        System.out.println("findById: " + tickets.findById(ticketId).orElseThrow());

        // После создания заявки выполняем назначение, комментарии и переходы статусов.
        User agent = users.findAll().stream()
                .filter(user -> user.getRole() == UserRole.SUPPORT_AGENT && user.isActive())
                .findFirst().orElseThrow(() -> new IllegalStateException("Нет активного сотрудника поддержки"));
        service.assignTicket(ticketId, agent.getId());
        comments.add(new TicketComment(ticketId, customer.getId(),
                "Прошу проверить обращение", false));
        service.changeStatus(ticketId, TicketStatus.RESOLVED, agent.getId());
        comments.add(new TicketComment(ticketId, agent.getId(),
                "Решение проверено сотрудником", true));
        service.changeStatus(ticketId, TicketStatus.CLOSED, agent.getId());

        showTicket(tickets, comments, ticketId);
        System.out.println("JOIN, карточка заявки:");
        TicketDetails details = tickets.findDetails().stream()
                .filter(item -> item.id() == ticketId).findFirst().orElseThrow();
        System.out.println(details);
        System.out.println("Отчёт по статусам: " + reports.countByStatus());
        System.out.println("Отчёт по категориям: " + reports.countByCategory());
        System.out.println("Нагрузка сотрудников: " + reports.activeByAgent());
        System.out.println("Просроченные заявки:");
        tickets.findOverdue().forEach(System.out::println);
        System.out.println("При следующем запуске заявка #" + ticketId
                + " появится в списке сохранённых заявок.");
    }

    // Повторно читает из БД заявку, её комментарии и историю изменений статуса.
    private static void showTicket(JdbcTicketRepository tickets, JdbcCommentRepository comments, long ticketId) {
        System.out.println("Заявка из БД: " + tickets.findById(ticketId).orElseThrow());
        System.out.println("Комментарии:");
        for (TicketComment comment : comments.findByTicketId(ticketId)) {
            System.out.println("  " + comment.getAuthorName() + " [" + comment.getAuthorRole() + "] "
                    + (comment.isInternal() ? "(внутренний) " : "") + comment.getText());
        }
        System.out.println("История статусов:");
        tickets.findStatusHistory(ticketId).forEach(System.out::println);
    }
}
