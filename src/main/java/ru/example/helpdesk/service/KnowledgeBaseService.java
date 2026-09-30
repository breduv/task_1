package ru.example.helpdesk.service;

import ru.example.helpdesk.model.KnowledgeArticle;
import ru.example.helpdesk.model.Ticket;
import ru.example.helpdesk.model.TicketStatus;
import ru.example.helpdesk.repository.TicketRepository;
import ru.example.helpdesk.repository.jdbc.JdbcKnowledgeBaseRepository;

public class KnowledgeBaseService {
    private final JdbcKnowledgeBaseRepository knowledgeBase;
    private final TicketRepository tickets;

    public KnowledgeBaseService(JdbcKnowledgeBaseRepository knowledgeBase, TicketRepository tickets) {
        this.knowledgeBase = knowledgeBase;
        this.tickets = tickets;
    }

    // Проверяет текст и добавляет статью в выбранную категорию
    public KnowledgeArticle addArticle(long categoryId, String title, String content) {
        if (categoryId <= 0) throw new IllegalArgumentException("Категория не указана");
        if (title == null || title.trim().length() < 3)
            throw new IllegalArgumentException("Название статьи должно содержать минимум 3 символа");
        if (content == null || content.isBlank())
            throw new IllegalArgumentException("Текст статьи пустой");
        return knowledgeBase.addArticle(categoryId, title.trim(), content.trim());
    }

    // Проверяет статус заявки перед привязкой статьи
    public void attachArticle(long ticketId, long articleId) {
        if (articleId <= 0) throw new IllegalArgumentException("Статья не указана");
        Ticket ticket = tickets.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Заявка не найдена"));
        if (ticket.getStatus() != TicketStatus.RESOLVED && ticket.getStatus() != TicketStatus.CLOSED)
            throw new IllegalStateException("Статью можно связать только с решённой или закрытой заявкой");
        knowledgeBase.linkToTicket(ticketId, articleId);
    }
}
