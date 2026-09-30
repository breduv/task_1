package ru.example.helpdesk.repository.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ru.example.helpdesk.config.DatabaseConfig;
import ru.example.helpdesk.model.ArticleCategory;
import ru.example.helpdesk.model.KnowledgeArticle;

public class JdbcKnowledgeBaseRepository {
    // Читает категории базы знаний
    public List<ArticleCategory> findCategories() {
        List<ArticleCategory> result = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT * FROM article_categories ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(new ArticleCategory(rs.getLong("id"), rs.getString("name")));
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка чтения категорий статей", e);
        }
    }

    // Находит статьи выбранной категории
    public List<KnowledgeArticle> findByCategory(long categoryId) {
        List<KnowledgeArticle> result = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT * FROM knowledge_articles WHERE category_id = ? ORDER BY id")) {
            ps.setLong(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapArticle(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка поиска статей по категории", e);
        }
    }

    // Читает статью, связанную с заявкой
    public Optional<KnowledgeArticle> findByTicket(long ticketId) {
        String sql = """
                SELECT a.* FROM knowledge_articles a
                JOIN tickets t ON t.knowledge_article_id = a.id WHERE t.id = ?
                """;
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapArticle(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка чтения статьи заявки", e);
        }
    }

    // Сохраняет новую статью и возвращает данные из базы
    public KnowledgeArticle addArticle(long categoryId, String title, String content) {
        String sql = """
                INSERT INTO knowledge_articles(category_id, title, content)
                VALUES (?, ?, ?) RETURNING *
                """;
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, categoryId);
            ps.setString(2, title);
            ps.setString(3, content);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Статья не сохранена");
                return mapArticle(rs);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка сохранения статьи", e);
        }
    }

    // Блокирует заявку и связывает её со статьёй в одной транзакции
    public void linkToTicket(long ticketId, long articleId) {
        TransactionTemplate.execute(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT status FROM tickets WHERE id = ? FOR UPDATE")) {
                ps.setLong(1, ticketId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("Заявка не найдена");
                    String status = rs.getString("status");
                    if (!"RESOLVED".equals(status) && !"CLOSED".equals(status))
                        throw new IllegalStateException("Статью можно связать только с решённой или закрытой заявкой");
                }
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT id FROM knowledge_articles WHERE id = ?")) {
                ps.setLong(1, articleId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("Статья не найдена");
                }
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE tickets SET knowledge_article_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                ps.setLong(1, articleId);
                ps.setLong(2, ticketId);
                ps.executeUpdate();
            }
            return null;
        });
    }

    // Собирает статью из строки результата
    private KnowledgeArticle mapArticle(ResultSet rs) throws SQLException {
        return new KnowledgeArticle(rs.getLong("id"), rs.getLong("category_id"),
                rs.getString("title"), rs.getString("content"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }
}
