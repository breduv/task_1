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
    /** INSERT возвращает ID и даты, которые PostgreSQL создал для новой заявки. */
    @Override
    public Ticket save(Ticket ticket) {
        String sql = """
                INSERT INTO tickets(title, description, status, priority,
                                    customer_id, assignee_id, category_id)
                VALUES (?, ?, ?::ticket_status, ?::ticket_priority, ?, ?, ?)
                RETURNING id, created_at, updated_at
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
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("INSERT не вернул id заявки");
                ticket.setId(rs.getLong("id"));
                ticket.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                ticket.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            }
            return ticket;
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось сохранить заявку", e);
        }
    }

    /** Читает одну заявку; ResultSet преобразуется в объект методом mapTicket. */
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

    /** Читает все заявки в порядке их ID. */
    @Override
    public List<Ticket> findAll() {
        return list("SELECT * FROM tickets ORDER BY id", null);
    }

    /** Подставляет статус как параметр SQL и сортирует подходящие заявки. */
    @Override
    public List<Ticket> findByStatus(TicketStatus status) {
        return list("SELECT * FROM tickets WHERE status = ?::ticket_status ORDER BY created_at DESC",
                status);
    }

    /** Ищет активные заявки, чей срок истёк с учётом приоритета. */
    @Override
    public List<Ticket> findOverdue() {
        String sql = """
                SELECT * FROM tickets
                WHERE status IN ('NEW', 'IN_PROGRESS')
                  AND CURRENT_TIMESTAMP > CASE priority
                    WHEN 'CRITICAL' THEN created_at + INTERVAL '30 minutes'
                    WHEN 'HIGH' THEN created_at + INTERVAL '2 hours'
                    WHEN 'MEDIUM' THEN created_at + INTERVAL '8 hours'
                    WHEN 'LOW' THEN created_at + INTERVAL '24 hours'
                  END
                ORDER BY created_at
                """;
        return list(sql, null);
    }

    /** Общий код чтения списка заявок для запросов с фильтром и без него. */
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

    /** Обновляет изменяемые поля заявки и её updated_at, не трогая статус. */
    @Override
    public void update(Ticket ticket) {
        if (ticket.getId() == null) throw new IllegalArgumentException("У заявки нет id");
        String sql = """
                UPDATE tickets SET title = ?, description = ?, priority = ?::ticket_priority,
                                   category_id = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? RETURNING updated_at
                """;
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, ticket.getTitle());
            ps.setString(2, ticket.getDescription());
            ps.setString(3, ticket.getPriority().name());
            setNullableLong(ps, 4, ticket.getCategoryId());
            ps.setLong(5, ticket.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Заявка не найдена: " + ticket.getId());
                ticket.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка обновления заявки", e);
        }
    }

    /** Удаляет заявку по ID и сообщает, была ли строка удалена. */
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

    /** В одной транзакции проверяет переход, меняет статус и пишет историю. */
    @Override
    public void changeStatus(long ticketId, TicketStatus newStatus, long changedByUserId) {
        String updateSql = """
                UPDATE tickets SET status = ?::ticket_status, updated_at = CURRENT_TIMESTAMP,
                closed_at = CASE WHEN ?::ticket_status = 'CLOSED' THEN CURRENT_TIMESTAMP
                                 ELSE closed_at END
                WHERE id = ?
                """;
        try (Connection connection = DatabaseConfig.getConnection()) {
            connection.setAutoCommit(false);
            try {
                // Блокировка не даёт двум транзакциям одновременно менять одну заявку.
                TicketStatus oldStatus = lockStatus(connection, ticketId);
                validateTransition(oldStatus, newStatus);
                try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                    ps.setString(1, newStatus.name());
                    ps.setString(2, newStatus.name());
                    ps.setLong(3, ticketId);
                    ps.executeUpdate();
                }
                insertHistory(connection, ticketId, oldStatus, newStatus, changedByUserId);
                // Изменение статуса и запись истории сохраняются только вместе.
                connection.commit();
            } catch (Exception e) {
                rollback(connection, e);
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка изменения статуса", e);
        }
    }

    /** Проверяет роль исполнителя и атомарно назначает его заявке. */
    @Override
    public void assignTicket(long ticketId, long supportAgentId) {
        String updateSql = """
                UPDATE tickets SET assignee_id = ?, status = 'IN_PROGRESS',
                                   updated_at = CURRENT_TIMESTAMP WHERE id = ?
                """;
        try (Connection connection = DatabaseConfig.getConnection()) {
            connection.setAutoCommit(false);
            try {
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
                connection.commit();
            } catch (Exception e) {
                rollback(connection, e);
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка назначения заявки", e);
        }
    }

    /** Читает статус с FOR UPDATE и блокирует заявку до конца транзакции. */
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

    /** Записывает переход статуса через то же соединение, что и основной UPDATE. */
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

    /** Разрешает только переходы статусов, перечисленные в методичке. */
    private void validateTransition(TicketStatus oldStatus, TicketStatus newStatus) {
        boolean allowed = switch (oldStatus) {
            case NEW -> newStatus == TicketStatus.IN_PROGRESS || newStatus == TicketStatus.CANCELLED;
            case IN_PROGRESS -> newStatus == TicketStatus.RESOLVED || newStatus == TicketStatus.CANCELLED;
            case RESOLVED -> newStatus == TicketStatus.CLOSED || newStatus == TicketStatus.IN_PROGRESS;
            case CLOSED, CANCELLED -> false;
        };
        if (!allowed) throw new IllegalStateException("Недопустимый переход: " + oldStatus + " -> " + newStatus);
    }

    /** Откатывает транзакцию и сохраняет ошибку отката как дополнительную причину. */
    private void rollback(Connection connection, Exception cause) {
        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            cause.addSuppressed(rollbackError);
        }
    }

    /** Возвращает хронологию переходов статуса конкретной заявки. */
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

    /** JOIN заменяет числовые ID клиента, исполнителя и категории их именами. */
    public List<TicketDetails> findDetails() {
        String sql = """
                SELECT t.id, t.title, t.status, t.priority, c.name AS category_name,
                       customer.name AS customer_name, assignee.name AS assignee_name, t.created_at
                FROM tickets t
                JOIN users customer ON customer.id = t.customer_id
                LEFT JOIN users assignee ON assignee.id = t.assignee_id
                LEFT JOIN categories c ON c.id = t.category_id
                ORDER BY t.created_at DESC
                """;
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

    /** Собирает Ticket из текущей строки ResultSet, включая nullable-поля. */
    private Ticket mapTicket(ResultSet rs) throws SQLException {
        Ticket ticket = new Ticket();
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
        Timestamp closed = rs.getTimestamp("closed_at");
        ticket.setClosedAt(closed == null ? null : closed.toLocalDateTime());
        return ticket;
    }

    /** Передаёт в SQL либо BIGINT, либо настоящий NULL вместо отсутствующего ID. */
    private void setNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) ps.setNull(index, Types.BIGINT);
        else ps.setLong(index, value);
    }
}
