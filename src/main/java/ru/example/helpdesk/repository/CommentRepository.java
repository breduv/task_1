package ru.example.helpdesk.repository;

import java.util.List;
import ru.example.helpdesk.model.TicketComment;

public interface CommentRepository {
    // Сохраняет комментарий и заполняет его ID и время создания.
    TicketComment add(TicketComment comment);
    // Возвращает комментарии заявки вместе с данными авторов.
    List<TicketComment> findByTicketId(long ticketId);
}
