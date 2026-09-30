package ru.example.helpdesk.repository.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import ru.example.helpdesk.config.DatabaseConfig;

public final class TransactionTemplate {
    private TransactionTemplate() {
    }

    @FunctionalInterface
    public interface Work<T> {
        T run(Connection connection) throws SQLException;
    }

    // Выполняет операции через одно соединение и сохраняет их вместе
    public static <T> T execute(Work<T> work) {
        try (Connection connection = DatabaseConfig.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException | Error e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка выполнения транзакции", e);
        }
    }
}
