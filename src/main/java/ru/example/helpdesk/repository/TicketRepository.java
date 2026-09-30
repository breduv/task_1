package ru.example.helpdesk.repository;

import java.util.List;
import java.util.Optional;
import ru.example.helpdesk.model.Ticket;
import ru.example.helpdesk.model.TicketStatus;

public interface TicketRepository {
    // Создаёт заявку и заполняет её ID, выданный PostgreSQL
    Ticket save(Ticket ticket);
    // Ищет заявку по ID и возвращает пустой Optional, если её нет
    Optional<Ticket> findById(long id);
    // Возвращает все заявки по возрастанию ID
    List<Ticket> findAll();
    // Отбирает заявки с указанным статусом
    List<Ticket> findByStatus(TicketStatus status);
    // Находит просроченные заявки со статусом NEW или IN_PROGRESS
    List<Ticket> findOverdue();
    // Обновляет текст, приоритет и категорию без смены статуса
    void update(Ticket ticket);
    // Удаляет заявку и возвращает true, если она существовала
    boolean deleteById(long id);
    // Меняет статус и записывает историю в одной транзакции
    void changeStatus(long ticketId, TicketStatus newStatus, long changedByUserId);
    // Назначает исполнителя и переводит новую заявку в работу
    void assignTicket(long ticketId, long supportAgentId);
}
