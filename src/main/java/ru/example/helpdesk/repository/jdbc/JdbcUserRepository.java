package ru.example.helpdesk.repository.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ru.example.helpdesk.config.DatabaseConfig;
import ru.example.helpdesk.model.Administrator;
import ru.example.helpdesk.model.Customer;
import ru.example.helpdesk.model.SupportAgent;
import ru.example.helpdesk.model.User;
import ru.example.helpdesk.model.UserRole;
import ru.example.helpdesk.repository.UserRepository;

public class JdbcUserRepository implements UserRepository {
    // Сохраняет пользователя и получает его ID и дату из RETURNING.
    @Override
    public User save(User user) {
        String sql = """
                INSERT INTO users(name, email, role, department_id)
                VALUES (?, ?, ?::user_role, ?) RETURNING id, created_at
                """;
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getRole().name());
            if (user.getDepartmentId() == null) ps.setNull(4, Types.BIGINT);
            else ps.setLong(4, user.getDepartmentId());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("INSERT не вернул id пользователя");
                user.setId(rs.getLong("id"));
                user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            }
            return user;
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка создания пользователя", e);
        }
    }

    // Находит пользователя по первичному ключу.
    @Override
    public Optional<User> findById(long id) {
        return findOne("SELECT * FROM users WHERE id = ?", id, null);
    }

    // Находит пользователя по уникальному email.
    @Override
    public Optional<User> findByEmail(String email) {
        return findOne("SELECT * FROM users WHERE email = ?", null, email);
    }

    // Выполняет один из двух параметризованных поисков и возвращает Optional.
    private Optional<User> findOne(String sql, Long id, String email) {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            if (id != null) ps.setLong(1, id);
            else ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapUser(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка поиска пользователя", e);
        }
    }

    // Читает всех пользователей по возрастанию ID.
    @Override
    public List<User> findAll() {
        List<User> result = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT * FROM users ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(mapUser(rs));
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка чтения пользователей", e);
        }
    }

    // По роли создаёт нужный подкласс User и заполняет данные из БД.
    private User mapUser(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String name = rs.getString("name");
        String email = rs.getString("email");
        UserRole role = UserRole.valueOf(rs.getString("role"));
        User user = switch (role) {
            case CUSTOMER -> new Customer(id, name, email);
            case SUPPORT_AGENT -> new SupportAgent(id, name, email);
            case ADMIN -> new Administrator(id, name, email);
        };
        long departmentId = rs.getLong("department_id");
        user.setDepartmentId(rs.wasNull() ? null : departmentId);
        user.setActive(rs.getBoolean("active"));
        user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return user;
    }
}
