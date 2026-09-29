package ru.example.helpdesk.service;

import ru.example.helpdesk.model.Ticket;
import ru.example.helpdesk.model.TicketPriority;
import ru.example.helpdesk.model.TicketStatus;
import ru.example.helpdesk.model.User;
import ru.example.helpdesk.model.UserRole;
import ru.example.helpdesk.repository.TicketRepository;
import ru.example.helpdesk.repository.UserRepository;
import ru.example.helpdesk.repository.jdbc.JdbcCatalogRepository;

public class TicketService {
    private final NotificationService notificationService;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final JdbcCatalogRepository catalogRepository;

    // Получает репозитории и способ отправки уведомлений через конструктор.
    public TicketService(TicketRepository ticketRepository, UserRepository userRepository,
                         JdbcCatalogRepository catalogRepository, NotificationService notificationService) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.catalogRepository = catalogRepository;
        this.notificationService = notificationService;
    }

    // Проверяет данные и сохраняет новую заявку со статусом NEW.
    public Ticket createTicket(String title, String description, TicketPriority priority,
                               long customerId, Long categoryId) {
        if (title == null || title.isBlank() || title.trim().length() < 3)
            throw new IllegalArgumentException("Тема заявки должна содержать минимум 3 символа");
        if (description == null || description.isBlank())
            throw new IllegalArgumentException("Описание заявки пустое");
        if (priority == null) throw new IllegalArgumentException("Приоритет не задан");
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Клиент не найден: " + customerId));
        if (customer.getRole() != UserRole.CUSTOMER || !customer.isActive())
            throw new IllegalArgumentException("Создателем должен быть активный клиент");
        if (categoryId != null && !catalogRepository.findCategoryById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Категория не найдена: " + categoryId)).active())
            throw new IllegalArgumentException("Категория неактивна");
        Ticket ticket = new Ticket();
        ticket.setTitle(title.trim());
        ticket.setDescription(description.trim());
        ticket.setStatus(TicketStatus.NEW);
        ticket.setPriority(priority);
        ticket.setCustomerId(customerId);
        ticket.setCategoryId(categoryId);
        return ticketRepository.save(ticket);
    }

    // Назначает активного сотрудника поддержки и сообщает об этом.
    public void assignTicket(long ticketId, long supportAgentId) {
        User agent = userRepository.findById(supportAgentId)
                .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден: " + supportAgentId));
        if (agent.getRole() != UserRole.SUPPORT_AGENT || !agent.isActive())
            throw new IllegalArgumentException("Исполнитель должен быть активным сотрудником поддержки");
        ticketRepository.assignTicket(ticketId, supportAgentId);
        notificationService.send("Заявка №" + ticketId + " назначена сотруднику " + agent.getName());
    }

    // Проверяет автора изменения и передаёт смену статуса репозиторию.
    public void changeStatus(long ticketId, TicketStatus newStatus, long changedByUserId) {
        User actor = userRepository.findById(changedByUserId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + changedByUserId));
        if (!actor.isActive()) throw new IllegalArgumentException("Пользователь неактивен");
        ticketRepository.changeStatus(ticketId, newStatus, changedByUserId);
        notificationService.send("Заявка №" + ticketId + ": новый статус " + newStatus);
    }


    // Отменяет заявку через общую проверяемую операцию смены статуса.
    public void cancelTicket(long ticketId, long changedByUserId) {
        changeStatus(ticketId, TicketStatus.CANCELLED, changedByUserId);
    }
}
