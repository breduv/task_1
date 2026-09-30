package ru.example.helpdesk.repository.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ru.example.helpdesk.config.DatabaseConfig;
import ru.example.helpdesk.model.Ticket;
import ru.example.helpdesk.model.TicketDetails;
import ru.example.helpdesk.model.TicketPriority;
import ru.example.helpdesk.model.TicketStatus;
import ru.example.helpdesk.model.TicketStatusHistory;
import ru.example.helpdesk.repository.TicketRepository;

public class JdbcTicketRepository implements TicketRepository {
    // Сохраняет заявку со сроком из политики SLA
    @Override
    public Ticket save(Ticket ticket) {
        String sql = """
                INSERT INTO tickets(title, description, status, priority, customer_id, assignee_id,
                                    category_id, due_at)
                SELECT ?, ?, ?::ticket_status, ?::ticket_priority, ?, ?, ?,
                       CURRENT_TIMESTAMP + make_interval(mins => s.resolve_minutes)
                FROM sla_policies s WHERE s.priority = ?::ticket_priority
                RETURNING id, created_at, updated_at, due_at
                """;
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, ticket.getTitle());
            ps.setString(2, ticket.getDescription());
            ps.setString(3, ticket.getStatus().name());
            ps.setString(4, ticket.getPriority().name());
            ps.setLong(5, ticket.getCustomerId());
            setNullableLong(ps, 6, ticket.getAssigneeId());
            setNullableLong(ps, 7, ticket.getCategoryId());
            ps.setString(8, ticket.getPriority().name());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Не найдена политика SLA для " + ticket.getPriority());
                ticket.setId(rs.getLong("id"));
                ticket.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                ticket.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                ticket.setDueAt(rs.getTimestamp("due_at").toLocalDateTime());
            }
            return ticket;
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось сохранить заявку", e);
        }
    }

    // Ищет заявку по ID и собирает объект Ticket из результата
    @Override
    public Optional<Ticket> findById(long id) {
        String sql = "SELECT * FROM tickets WHERE id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapTicket(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка поиска заявки", e);
        }
    }

    // Читает все заявки в порядке их ID
    @Override
    public List<Ticket> findAll() {
        return list("SELECT * FROM tickets ORDER BY id", null);
    }

    // Находит заявки с нужным статусом
    @Override
    public List<Ticket> findByStatus(TicketStatus status) {
        return list("SELECT * FROM tickets WHERE status = ?::ticket_status ORDER BY created_at DESC",
                status);
    }

    // Находит открытые заявки с истёкшим сроком из базы
    @Override
    public List<Ticket> findOverdue() {
        String sql = """
                SELECT * FROM tickets
                WHERE status IN ('NEW', 'IN_PROGRESS')
                  AND due_at < CURRENT_TIMESTAMP
                ORDER BY due_at
                """;
        return list(sql, null);
    }

    // Читает список заявок с фильтром или без него
    private List<Ticket> list(String sql, TicketStatus status) {
        List<Ticket> result = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            if (status != null) ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapTicket(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка чтения заявок", e);
        }
    }

    // Обновляет данные заявки и время изменения, не меняя статус
    @Override
    public void update(Ticket ticket) {
        if (ticket.getId() == null) throw new IllegalArgumentException("У заявки нет id");
        String sql = """
                UPDATE tickets SET title = ?, description = ?, priority = ?::ticket_priority,
                                   category_id = ?, updated_at = CURRENT_TIMESTAMP,
                                   due_at = CASE WHEN priority = ?::ticket_priority THEN due_at
                                       ELSE created_at + make_interval(mins =>
                                           (SELECT resolve_minutes FROM sla_policies
                                            WHERE priority = ?::ticket_priority)) END
                WHERE id = ? RETURNING updated_at, due_at
                """;
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, ticket.getTitle());
            ps.setString(2, ticket.getDescription());
            ps.setString(3, ticket.getPriority().name());
            setNullableLong(ps, 4, ticket.getCategoryId());
            ps.setString(5, ticket.getPriority().name());
            ps.setString(6, ticket.getPriority().name());
            ps.setLong(7, ticket.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Заявка не найдена: " + ticket.getId());
                ticket.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                ticket.setDueAt(rs.getTimestamp("due_at").toLocalDateTime());
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка обновления заявки", e);
        }
    }

    // Удаляет заявку по ID и сообщает, была ли строка удалена
    @Override
    public boolean deleteById(long id) {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement("DELETE FROM tickets WHERE id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка удаления заявки", e);
        }
    }

    // Меняет статус и добавляет запись в историю в одной транзакции
    @Override
    public void changeStatus(long ticketId, TicketStatus newStatus, long changedByUserId) {
        String updateSql = """
                UPDATE tickets SET status = ?::ticket_status, updated_at = CURRENT_TIMESTAMP,
                closed_at = CASE WHEN ?::ticket_status = 'CLOSED' THEN CURRENT_TIMESTAMP
                                 ELSE closed_at END
                WHERE id = ?
                """;
        TransactionTemplate.execute(connection -> {
            TicketStatus oldStatus = lockStatus(connection, ticketId);
            validateTransition(oldStatus, newStatus);
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                ps.setString(1, newStatus.name());
                ps.setString(2, newStatus.name());
                ps.setLong(3, ticketId);
                ps.executeUpdate();
            }
            insertHistory(connection, ticketId, oldStatus, newStatus, changedByUserId);
            return null;
        });
    }

    // Проверяет исполнителя и назначает его заявке
    @Override
    public void assignTicket(long ticketId, long supportAgentId) {
        String updateSql = """
                UPDATE tickets SET assignee_id = ?, status = 'IN_PROGRESS',
                                   updated_at = CURRENT_TIMESTAMP WHERE id = ?
                """;
        TransactionTemplate.execute(connection -> {
            TicketStatus oldStatus = lockStatus(connection, ticketId);
            if (oldStatus != TicketStatus.NEW && oldStatus != TicketStatus.IN_PROGRESS) {
                throw new IllegalStateException("Нельзя назначить заявку в статусе " + oldStatus);
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT role FROM users WHERE id = ? AND active = TRUE")) {
                ps.setLong(1, supportAgentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || !"SUPPORT_AGENT".equals(rs.getString("role"))) {
                        throw new IllegalArgumentException("Исполнитель должен быть активным сотрудником поддержки");
                    }
                }
            }
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                ps.setLong(1, supportAgentId);
                ps.setLong(2, ticketId);
                ps.executeUpdate();
            }
            if (oldStatus == TicketStatus.NEW) {
                insertHistory(connection, ticketId, oldStatus, TicketStatus.IN_PROGRESS, supportAgentId);
            }
            return null;
        });
    }

    // Блокирует заявку и читает её текущий статус
    private TicketStatus lockStatus(Connection connection, long ticketId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT status FROM tickets WHERE id = ? FOR UPDATE")) {
            ps.setLong(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Заявка не найдена: " + ticketId);
                return TicketStatus.valueOf(rs.getString("status"));
            }
        }
    }

    // Добавляет переход статуса в историю через то же соединение
    private void insertHistory(Connection connection, long ticketId, TicketStatus oldStatus,
                               TicketStatus newStatus, long changedByUserId) throws SQLException {
        String sql = """
                INSERT INTO ticket_status_history(ticket_id, old_status, new_status, changed_by_id)
                VALUES (?, ?::ticket_status, ?::ticket_status, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, ticketId);
            ps.setString(2, oldStatus.name());
            ps.setString(3, newStatus.name());
            ps.setLong(4, changedByUserId);
            ps.executeUpdate();
        }
    }

    // Проверяет, можно ли перейти из текущего статуса в новый
    private void validateTransition(TicketStatus oldStatus, TicketStatus newStatus) {
        boolean allowed = switch (oldStatus) {
            case NEW -> newStatus == TicketStatus.IN_PROGRESS || newStatus == TicketStatus.CANCELLED;
            case IN_PROGRESS -> newStatus == TicketStatus.RESOLVED || newStatus == TicketStatus.CANCELLED;
            case RESOLVED -> newStatus == TicketStatus.CLOSED || newStatus == TicketStatus.IN_PROGRESS;
            case CLOSED, CANCELLED -> false;
        };
        if (!allowed) throw new IllegalStateException("Недопустимый переход: " + oldStatus + " -> " + newStatus);
    }

    // Читает историю статусов заявки по порядку
    public List<TicketStatusHistory> findStatusHistory(long ticketId) {
        String sql = """
                SELECT id, ticket_id, old_status, new_status, changed_by_id, changed_at
                FROM ticket_status_history WHERE ticket_id = ? ORDER BY changed_at, id
                """;
        List<TicketStatusHistory> result = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String old = rs.getString("old_status");
                    long changedBy = rs.getLong("changed_by_id");
                    boolean changedByWasNull = rs.wasNull();
                    result.add(new TicketStatusHistory(rs.getLong("id"), rs.getLong("ticket_id"),
                            old == null ? null : TicketStatus.valueOf(old),
                            TicketStatus.valueOf(rs.getString("new_status")),
                            changedByWasNull ? null : changedBy,
                            rs.getTimestamp("changed_at").toLocalDateTime()));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка чтения истории", e);
        }
    }

    // Читает карточки заявок из представления
    public List<TicketDetails> findDetails() {
        String sql = "SELECT * FROM ticket_details ORDER BY created_at DESC, id DESC";
        List<TicketDetails> result = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new TicketDetails(rs.getLong("id"), rs.getString("title"),
                        TicketStatus.valueOf(rs.getString("status")),
                        TicketPriority.valueOf(rs.getString("priority")),
                        rs.getString("customer_name"), rs.getString("assignee_name"),
                        rs.getString("category_name"), rs.getTimestamp("created_at").toLocalDateTime()));
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка JOIN-запроса", e);
        }
    }

    // Собирает заявку из строки результата, учитывая пустые поля
    private Ticket mapTicket(ResultSet rs) throws SQLException {
        Ticket ticket = new Ticket();
        ticket.setKnowledgeArticleId(rs.getObject("knowledge_article_id", Long.class));
        ticket.setId(rs.getLong("id"));
        ticket.setTitle(rs.getString("title"));
        ticket.setDescription(rs.getString("description"));
        ticket.setStatus(TicketStatus.valueOf(rs.getString("status")));
        ticket.setPriority(TicketPriority.valueOf(rs.getString("priority")));
        ticket.setCustomerId(rs.getLong("customer_id"));
        long assigneeId = rs.getLong("assignee_id");
        ticket.setAssigneeId(rs.wasNull() ? null : assigneeId);
        long categoryId = rs.getLong("category_id");
        ticket.setCategoryId(rs.wasNull() ? null : categoryId);
        ticket.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        ticket.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        ticket.setDueAt(rs.getTimestamp("due_at").toLocalDateTime());
        Timestamp closed = rs.getTimestamp("closed_at");
        ticket.setClosedAt(closed == null ? null : closed.toLocalDateTime());
        return ticket;
    }

    // Передаёт ID в запрос или SQL NULL, если ID нет
    private void setNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) ps.setNull(index, Types.BIGINT);
        else ps.setLong(index, value);
    }
}
