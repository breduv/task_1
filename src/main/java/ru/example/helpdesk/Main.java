package ru.example.helpdesk;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.OptionalDouble;

import ru.example.helpdesk.config.DatabaseConfig;
import ru.example.helpdesk.model.AgentTicketStats;
import ru.example.helpdesk.model.Category;
import ru.example.helpdesk.model.SlaPolicy;
import ru.example.helpdesk.model.Ticket;
import ru.example.helpdesk.model.TicketComment;
import ru.example.helpdesk.model.TicketDetails;
import ru.example.helpdesk.model.TicketStatus;
import ru.example.helpdesk.model.TicketStatusHistory;
import ru.example.helpdesk.model.User;
import ru.example.helpdesk.model.UserRole;
import ru.example.helpdesk.repository.jdbc.JdbcCatalogRepository;
import ru.example.helpdesk.repository.jdbc.JdbcCommentRepository;
import ru.example.helpdesk.repository.jdbc.JdbcReportRepository;
import ru.example.helpdesk.repository.jdbc.JdbcSlaPolicyRepository;
import ru.example.helpdesk.repository.jdbc.JdbcTicketRepository;
import ru.example.helpdesk.repository.jdbc.JdbcUserRepository;
import ru.example.helpdesk.service.ConsoleNotificationService;
import ru.example.helpdesk.service.TicketService;

public class Main {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    // Запускает проверку работы приложения с базой данных
    public static void main(String[] args) {
        JdbcTicketRepository tickets = new JdbcTicketRepository();
        JdbcUserRepository users = new JdbcUserRepository();
        JdbcCatalogRepository catalog = new JdbcCatalogRepository();
        JdbcCommentRepository comments = new JdbcCommentRepository();
        JdbcReportRepository reports = new JdbcReportRepository();
        JdbcSlaPolicyRepository slaPolicies = new JdbcSlaPolicyRepository();
        TicketService service = new TicketService(tickets, users, catalog, new ConsoleNotificationService());

        try (Connection connection = DatabaseConfig.getConnection()) {
            System.out.println("Подключение к PostgreSQL успешно");
            System.out.println("AutoCommit = " + connection.getAutoCommit());
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось подключиться к PostgreSQL", e);
        }

        // Сначала показываем заявки, которые уже хранятся в базе
        System.out.println();
        System.out.println("Сохранённые заявки:");
        List<Ticket> savedTickets = tickets.findAll();
        if (savedTickets.isEmpty()) System.out.println("  Пока нет заявок");
        else savedTickets.forEach(ticket -> System.out.println("  " + ticket));

        // Находим клиента и создаём новую заявку
        System.out.println();
        User customer = users.findByEmail("anna@example.org")
                .orElseThrow(() -> new IllegalStateException("Нет тестового клиента anna@example.org"));
        if (customer.getRole() != UserRole.CUSTOMER)
            throw new IllegalStateException("Найденный пользователь не является клиентом");
        System.out.println("Клиент найденный по email: " + customer.getName());

        System.out.println();
        List<Category> categories = catalog.findAllCategories();
        System.out.println("Категории:");
        categories.forEach(category -> System.out.println("  #" + category.id() + " " + category.name()));
        Category category = categories.stream().filter(Category::active).findFirst()
                .orElseThrow(() -> new IllegalStateException("Нет активной категории"));

        System.out.println();
        System.out.println("Политики SLA:");
        for (SlaPolicy policy : slaPolicies.findAll()) {
            System.out.println("  " + policy.priority() + " | ответ: " + policy.responseMinutes()
                    + " мин | решение: " + policy.resolveMinutes() + " мин");
        }

        Ticket created = service.createTicket("Снова WIFI не работает",
                "СНОВА ВАЙ ФАЙ НЕ РАБОТАЕТ, ПОМОГИТЕ!",
                category.defaultPriority(), customer.getId(), category.id());
        long ticketId = created.getId();
        System.out.println();
        System.out.println("Создана заявка #" + ticketId);
        System.out.println("  " + tickets.findById(ticketId).orElseThrow());

        // Назначаем сотрудника, добавляем комментарии и меняем статусы
        System.out.println();
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

        System.out.println();
        showTicket(tickets, comments, ticketId);

        TicketDetails details = tickets.findDetails().stream()
                .filter(item -> item.id() == ticketId).findFirst().orElseThrow();
        System.out.println();
        System.out.println("Карточка заявки:");
        System.out.println("  #" + details.id() + " " + details.title());
        System.out.println("  Статус: " + details.status() + " | Приоритет: " + details.priority());
        System.out.println("  Клиент: " + details.customerName()
                + " | Исполнитель: " + details.assigneeName()
                + " | Категория: " + details.categoryName());
        System.out.println("  Создана: " + details.createdAt().format(DATE_FORMAT));

        System.out.println();
        System.out.println("Отчёты:");
        System.out.println("  По статусам: " + reports.countByStatus());
        System.out.println("  По категориям: " + reports.countByCategory());
        System.out.println("  Нагрузка сотрудников: " + reports.activeByAgent());

        System.out.println();
        System.out.println("Статистика сотрудников:");
        for (AgentTicketStats stats : reports.ticketCountsByAgent()) {
            System.out.println("  " + stats.agentName() + " | активные: " + stats.active()
                    + " | решённые: " + stats.resolved() + " | закрытые: " + stats.closed());
        }

        System.out.println();
        System.out.println("Среднее время решения:");
        OptionalDouble average = reports.averageResolutionMinutes();
        System.out.println("  Все закрытые заявки: " + (average.isPresent()
                ? String.format("%.4f мин", average.getAsDouble()) : "нет закрытых заявок"));
        reports.averageResolutionMinutesByCategory().forEach((name, minutes) ->
                System.out.println("  " + name + ": " + String.format("%.4f мин", minutes)));

        System.out.println();
        System.out.println("Просроченные заявки:");
        List<Ticket> overdue = tickets.findOverdue();
        if (overdue.isEmpty()) System.out.println("  Нет");
        else overdue.forEach(ticket -> System.out.println("  " + ticket));
    }

    // Показывает заявку, комментарии и историю статусов из базы
    private static void showTicket(JdbcTicketRepository tickets, JdbcCommentRepository comments, long ticketId) {
        System.out.println("Заявка: " + tickets.findById(ticketId).orElseThrow());
        System.out.println();
        System.out.println("Комментарии:");
        for (TicketComment comment : comments.findByTicketId(ticketId)) {
            System.out.println("  " + comment.getAuthorName() + " [" + comment.getAuthorRole() + "] "
                    + (comment.isInternal() ? "(внутренний) " : "") + comment.getText());
        }
        System.out.println();
        System.out.println("История статусов:");
        for (TicketStatusHistory entry : tickets.findStatusHistory(ticketId)) {
            System.out.println("  " + entry.oldStatus() + " → " + entry.newStatus()
                    + " | " + entry.changedAt().format(DATE_FORMAT)
                    + " | пользователь #" + entry.changedById());
        }
    }
}
