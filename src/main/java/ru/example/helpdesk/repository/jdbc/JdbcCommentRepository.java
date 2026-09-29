package ru.example.helpdesk.repository.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ru.example.helpdesk.config.DatabaseConfig;
import ru.example.helpdesk.model.TicketComment;
import ru.example.helpdesk.model.UserRole;
import ru.example.helpdesk.repository.CommentRepository;

public class JdbcCommentRepository implements CommentRepository {
    /** Добавляет комментарий и возвращает его ID и дату создания из БД. */
    @Override
    public TicketComment add(TicketComment comment) {
        String sql = """
                INSERT INTO ticket_comments(ticket_id, author_id, text, internal)
                VALUES (?, ?, ?, ?) RETURNING id, created_at
                """;
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, comment.getTicketId());
            ps.setLong(2, comment.getAuthorId());
            ps.setString(3, comment.getText());
            ps.setBoolean(4, comment.isInternal());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("INSERT не вернул id комментария");
                comment.setId(rs.getLong("id"));
                comment.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            }
            return comment;
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка добавления комментария", e);
        }
    }

    /** Читает комментарии заявки с именем и ролью каждого автора. */
    @Override
    public List<TicketComment> findByTicketId(long ticketId) {
        String sql = """
                SELECT c.id, c.ticket_id, c.author_id, c.text, c.internal, c.created_at,
                       u.name AS author_name, u.role AS author_role
                FROM ticket_comments c JOIN users u ON u.id = c.author_id
                WHERE c.ticket_id = ? ORDER BY c.created_at, c.id
                """;
        List<TicketComment> result = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TicketComment comment = new TicketComment(rs.getLong("ticket_id"),
                            rs.getLong("author_id"), rs.getString("text"), rs.getBoolean("internal"));
                    comment.setId(rs.getLong("id"));
                    comment.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    comment.setAuthorName(rs.getString("author_name"));
                    comment.setAuthorRole(UserRole.valueOf(rs.getString("author_role")));
                    result.add(comment);
                }
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка чтения комментариев", e);
        }
    }
}
