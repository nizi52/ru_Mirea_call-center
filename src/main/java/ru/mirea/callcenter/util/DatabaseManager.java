package ru.mirea.callcenter.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DatabaseManager {
    private static final Properties PROPERTIES = loadProperties();
    private DatabaseManager() { }
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(PROPERTIES.getProperty("db.url"), PROPERTIES.getProperty("db.user"), PROPERTIES.getProperty("db.password"));
    }
    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = DatabaseManager.class.getClassLoader().getResourceAsStream("database.properties")) {
            if (input == null) throw new IllegalStateException("Не найден database.properties");
            properties.load(input);
            return properties;
        } catch (IOException e) { throw new IllegalStateException("Не удалось прочитать настройки БД", e); }
    }
}
