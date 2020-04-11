package de.untenrechts.urhome.database;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class DatabaseConfig {

    public static final String JDBC_URL_KEY = "jdbcUrl";
    public static final String DRIVER_CLASS_KEY = "driverClass";
    public static final String USER_KEY = "user";
    public static final String PASSWORD_KEY = "password";

    private final String jdbcUrl;
    private final String driverClass;
    private final String user;
    private final String password;

}
