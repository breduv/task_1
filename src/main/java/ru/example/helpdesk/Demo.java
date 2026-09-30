package ru.example.helpdesk;

import java.sql.PreparedStatement;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

import ru.example.helpdesk.model.AgentTicketStats;
import ru.example.helpdesk.model.ArticleCategory;
import ru.example.helpdesk.model.KnowledgeArticle;
import ru.example.helpdesk.model.SlaPolicy;
import ru.example.helpdesk.model.Ticket;
import ru.example.helpdesk.model.TicketComment;
import ru.example.helpdesk.model.TicketDetails;
import ru.example.helpdesk.model.TicketStatus;
import ru.example.helpdesk.model.User;
import ru.example.helpdesk.model.UserRole;
import ru.example.helpdesk.repository.jdbc.JdbcCatalogRepository;
import ru.example.helpdesk.service.ConsoleNotificationService;
import ru.example.helpdesk.service.TicketService;
import ru.example.helpdesk.repository.jdbc.JdbcCommentRepository;
import ru.example.helpdesk.repository.jdbc.JdbcKnowledgeBaseRepository;
import ru.example.helpdesk.service.KnowledgeBaseService;
import ru.example.helpdesk.repository.jdbc.JdbcReportRepository;
import ru.example.helpdesk.repository.jdbc.JdbcSlaPolicyRepository;
import ru.example.helpdesk.repository.jdbc.JdbcTicketRepository;
import ru.example.helpdesk.repository.jdbc.JdbcUserRepository;
import ru.example.helpdesk.repository.jdbc.TransactionTemplate;

public class Demo {
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss.SSSSSS");
    private static final DateTimeFormatter TICKET_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    public static void main(String[] args) {
        JdbcUserRepository users = new JdbcUserRepository();
        JdbcTicketRepository tickets = new JdbcTicketRepository();
        JdbcCommentRepository comments = new JdbcCommentRepository();
        JdbcSlaPolicyRepository slaPolicies = new JdbcSlaPolicyRepository();
        JdbcReportRepository reports = new JdbcReportRepository();

        System.out.println("=== Задание 1. Поиск клиента по email ===");
        showResult(users, "anna@example.org");
        System.out.println();
        showResult(users, "missing@example.invalid");

        System.out.println();
        System.out.println("=== Задание 3. Дата последнего изменения ===");
        showUpdatedAt(tickets);

        System.out.println();
        System.out.println("=== Задание 6. Комментарии клиента ===");
        showExternalComments(tickets, comments);

        System.out.println();
        System.out.println("=== Задание 7. Проверка роли исполнителя ===");
        showAssignment(tickets, users);

        System.out.println();
        System.out.println("=== Задание 9. Политики SLA ===");
        showSlaPolicies(slaPolicies);

        System.out.println();
        System.out.println("=== Задание 10. Дедлайн заявки ===");
        showDeadlines(tickets);

        System.out.println();
        System.out.println("=== Задание 15. Статистика сотрудников ===");
        showAgentStats(reports);

        System.out.println();
        System.out.println("=== Задание 16. Среднее время решения ===");
        showResolutionTime(reports);

        System.out.println();
        System.out.println("=== Задание 20. Шаблон транзакций ===");
        showTransactions(tickets);

        System.out.println();
        System.out.println("=== Задание 22. Представление ticket_details ===");
        showView(tickets);

        System.out.println();
        System.out.println("=== Задание 42. Вариант A. База знаний ===");
        showKnowledgeBase(tickets);
    }

    // Проверяет отказ для клиента и успешное назначение сотрудника
    private static void showAssignment(JdbcTicketRepository tickets, JdbcUserRepository users) {
        Ticket ticket = tickets.findAll().stream()
                .filter(item -> item.getStatus() == TicketStatus.IN_PROGRESS)
                .findFirst().orElseThrow(() -> new IllegalStateException("Нет заявки в работе"));
        User customer = users.findById(ticket.getCustomerId()).orElseThrow();
        User currentAgent = users.findById(ticket.getAssigneeId()).orElseThrow();
        User agent = users.findAll().stream()
                .filter(user -> user.getRole() == UserRole.SUPPORT_AGENT && user.isActive()
                        && user.getId() != currentAgent.getId())
                .findFirst().orElseThrow(() -> new IllegalStateException("Нет активного исполнителя заявки"));
        TicketService service = new TicketService(tickets, users, new JdbcCatalogRepository(),
                new ConsoleNotificationService());
        System.out.println("Заявка #" + ticket.getId() + " | исполнитель: " + currentAgent.getName());
        System.out.println();
        System.out.println("Попытка назначить клиента " + customer.getName() + ":");
        try {
            service.assignTicket(ticket.getId(), customer.getId());
            throw new IllegalStateException("Клиент назначен исполнителем");
        } catch (IllegalArgumentException e) {
            if (!"Исполнитель должен быть активным сотрудником поддержки".equals(e.getMessage())) throw e;
            System.out.println("Ошибка: " + e.getMessage());
        }
        if (!Long.valueOf(currentAgent.getId()).equals(tickets.findById(ticket.getId()).orElseThrow().getAssigneeId())) {
            throw new IllegalStateException("Исполнитель изменился после отказа");
        }
        System.out.println();
        service.assignTicket(ticket.getId(), agent.getId());
        Ticket assigned = tickets.findById(ticket.getId()).orElseThrow();
        if (!Long.valueOf(agent.getId()).equals(assigned.getAssigneeId())) {
            throw new IllegalStateException("Исполнитель не сохранён");
        }
        System.out.println("Новый исполнитель: " + users.findById(assigned.getAssigneeId()).orElseThrow().getName());
    }

    // Показывает статьи и привязывает одну из них к решённой заявке
    private static void showKnowledgeBase(JdbcTicketRepository tickets) {
        JdbcKnowledgeBaseRepository knowledgeBase = new JdbcKnowledgeBaseRepository();
        KnowledgeBaseService service = new KnowledgeBaseService(knowledgeBase, tickets);
        List<ArticleCategory> categories = knowledgeBase.findCategories();
        System.out.println("Категории статей:");
        categories.forEach(category -> System.out.println("  #" + category.id() + " " + category.name()));

        ArticleCategory category = categories.stream()
                .filter(item -> "Сеть".equals(item.name())).findFirst().orElseThrow();
        List<KnowledgeArticle> articles = knowledgeBase.findByCategory(category.id());
        System.out.println();
        System.out.println("Статьи категории " + category.name() + ":");
        articles.forEach(article -> {
            System.out.println("  #" + article.id() + " " + article.title());
            System.out.println("    " + article.content());
        });

        List<Ticket> saved = tickets.findAll();
        Ticket resolved = saved.stream()
                .filter(ticket -> ticket.getStatus() == TicketStatus.RESOLVED
                        || ticket.getStatus() == TicketStatus.CLOSED)
                .findFirst().orElseThrow();
        KnowledgeArticle article = articles.stream().findFirst().orElseThrow();
        service.attachArticle(resolved.getId(), article.id());
        KnowledgeArticle linked = knowledgeBase.findByTicket(resolved.getId()).orElseThrow();
        System.out.println();
        System.out.println("Заявка #" + resolved.getId() + " | " + resolved.getStatus());
        System.out.println("Статья сохранена: " + linked.title());

        Ticket open = saved.stream()
                .filter(ticket -> ticket.getStatus() == TicketStatus.NEW
                        || ticket.getStatus() == TicketStatus.IN_PROGRESS)
                .findFirst().orElseThrow();
        Optional<KnowledgeArticle> before = knowledgeBase.findByTicket(open.getId());
        try {
            service.attachArticle(open.getId(), article.id());
            throw new IllegalStateException("Открытая заявка приняла статью");
        } catch (IllegalStateException e) {
            if (!"Статью можно связать только с решённой или закрытой заявкой".equals(e.getMessage())) throw e;
            System.out.println();
            System.out.println("Заявка #" + open.getId() + " | " + open.getStatus());
            System.out.println("Отказ: " + e.getMessage());
        }
        if (!before.equals(knowledgeBase.findByTicket(open.getId()))) {
            throw new IllegalStateException("Связь открытой заявки изменилась");
        }
    }

    // Показывает результат поиска по одному email
    private static void showResult(JdbcUserRepository users, String email) {
        Optional<User> result = users.findByEmail(email);
        System.out.println("Поиск: " + email);

        if (result.isPresent()) {
            User user = result.get();
            System.out.println("Найден: #" + user.getId() + " " + user.getName()
                    + " | роль: " + user.getRole());
        } else {
            System.out.println("Не найден");
        }
    }

    // Обновляет существующую заявку и показывает дату до и после UPDATE
    private static void showUpdatedAt(JdbcTicketRepository tickets) {
        List<Ticket> saved = tickets.findAll();
        Ticket ticket = saved.stream()
                .filter(item -> item.getStatus() == TicketStatus.NEW
                        || item.getStatus() == TicketStatus.IN_PROGRESS)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Нет открытой заявки для проверки UPDATE"));

        System.out.println("Заявка #" + ticket.getId() + ": " + ticket.getTitle());
        System.out.println("До UPDATE:    " + ticket.getUpdatedAt().format(DATE_FORMAT));
        tickets.update(ticket);
        Ticket updated = tickets.findById(ticket.getId()).orElseThrow();
        System.out.println("После UPDATE: " + updated.getUpdatedAt().format(DATE_FORMAT));
    }

    // Показывает только общедоступные комментарии к существующей заявке
    private static void showExternalComments(JdbcTicketRepository tickets,
                                             JdbcCommentRepository comments) {
        Ticket ticket = tickets.findAll().stream()
                .filter(item -> comments.findByTicketId(item.getId()).stream()
                        .anyMatch(comment -> !comment.isInternal()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Нет заявки с общедоступными комментариями"));

        System.out.println("Заявка #" + ticket.getId() + ": " + ticket.getTitle());
        for (TicketComment comment : comments.findByTicketId(ticket.getId())) {
            if (!comment.isInternal()) {
                System.out.println(comment.getAuthorName() + ": " + comment.getText());
            }
        }
    }

    // Показывает сроки ответа и решения для каждого приоритета
    private static void showSlaPolicies(JdbcSlaPolicyRepository slaPolicies) {
        List<SlaPolicy> policies = slaPolicies.findAll();
        if (policies.isEmpty()) System.out.println("Политик SLA нет");
        for (SlaPolicy policy : policies) {
            System.out.println("#" + policy.id() + " " + policy.name() + " | " + policy.priority());
            System.out.println("  Ответ: " + policy.responseMinutes()
                    + " мин | Решение: " + policy.resolveMinutes() + " мин");
        }
    }

    // Показывает сохранённые сроки заявок и список просроченных
    private static void showDeadlines(JdbcTicketRepository tickets) {
        System.out.println("Сроки заявок:");
        List<Ticket> saved = tickets.findAll();
        if (saved.isEmpty()) System.out.println("  Заявок нет");
        for (Ticket ticket : saved) {
            System.out.println("  #" + ticket.getId() + " " + ticket.getTitle()
                    + " | " + ticket.getPriority() + " | " + ticket.getStatus());
            System.out.println("    Создана: " + ticket.getCreatedAt().format(TICKET_DATE_FORMAT)
                    + " | Срок: " + ticket.getDueAt().format(TICKET_DATE_FORMAT));
        }

        System.out.println();
        System.out.println("Просроченные заявки:");
        List<Ticket> overdue = tickets.findOverdue();
        if (overdue.isEmpty()) System.out.println("  Нет");
        else overdue.forEach(ticket -> System.out.println("  " + ticket));
    }

    // Показывает количество заявок каждого сотрудника по статусам
    private static void showAgentStats(JdbcReportRepository reports) {
        List<AgentTicketStats> stats = reports.ticketCountsByAgent();
        if (stats.isEmpty()) System.out.println("Сотрудников поддержки нет");
        for (AgentTicketStats agent : stats) {
            System.out.println(agent.agentName() + " | Активные: " + agent.active()
                    + " | Решённые: " + agent.resolved() + " | Закрытые: " + agent.closed());
        }
    }

    // Показывает среднее время решения в целом и по категориям
    private static void showResolutionTime(JdbcReportRepository reports) {
        OptionalDouble average = reports.averageResolutionMinutes();
        System.out.println("Все закрытые заявки: " + (average.isPresent()
                ? String.format("%.4f мин", average.getAsDouble()) : "нет закрытых заявок"));

        System.out.println();
        System.out.println("По категориям:");
        Map<String, Double> categories = reports.averageResolutionMinutesByCategory();
        if (categories.isEmpty()) System.out.println("  Нет закрытых заявок");
        categories.forEach((name, minutes) ->
                System.out.println("  " + name + ": " + String.format("%.4f мин", minutes)));
    }

    // Проверяет сохранение транзакции и откат после ошибки
    private static void showTransactions(JdbcTicketRepository tickets) {
        Ticket ticket = tickets.findAll().stream().findFirst().orElseThrow();
        System.out.println("Заявка #" + ticket.getId());
        int changed = TransactionTemplate.execute(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE tickets SET updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                ps.setLong(1, ticket.getId());
                return ps.executeUpdate();
            }
        });
        if (changed != 1) throw new IllegalStateException("Заявка не обновлена");
        System.out.println("COMMIT: время изменения сохранено");

        String originalTitle = tickets.findById(ticket.getId()).orElseThrow().getTitle();
        System.out.println("До транзакции: " + originalTitle);
        try {
            TransactionTemplate.execute(connection -> {
                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE tickets SET title = ? WHERE id = ?")) {
                    ps.setString(1, "Проверка отката");
                    ps.setLong(2, ticket.getId());
                    ps.executeUpdate();
                }
                System.out.println("Внутри транзакции: Проверка отката");
                throw new IllegalStateException("Проверка отката");
            });
        } catch (IllegalStateException e) {
            if (!"Проверка отката".equals(e.getMessage())) throw e;
            System.out.println("ROLLBACK: изменения отменены после ошибки");
        }
        String restoredTitle = tickets.findById(ticket.getId()).orElseThrow().getTitle();
        if (!originalTitle.equals(restoredTitle)) throw new IllegalStateException("Откат не восстановил тему");
        System.out.println("После отката: " + restoredTitle);
    }

    // Показывает карточки, прочитанные через VIEW
    private static void showView(JdbcTicketRepository tickets) {
        List<TicketDetails> details = tickets.findDetails();
        if (details.isEmpty()) System.out.println("Заявок нет");
        for (TicketDetails ticket : details) {
            System.out.println("#" + ticket.id() + " " + ticket.title()
                    + " | " + ticket.status() + " | " + ticket.priority());
            System.out.println("  Клиент: " + ticket.customerName()
                    + " | Исполнитель: " + (ticket.assigneeName() == null ? "не назначен" : ticket.assigneeName())
                    + " | Категория: " + (ticket.categoryName() == null ? "не указана" : ticket.categoryName()));
        }
    }
}
