package de.untenrechts.urhome.database;

public class DatabaseQueries {

    public static final String SQL_TEST_CONNECTION = "SELECT 1 " +
            "FROM purchase_mapping pm " +
            "JOIN purchases p ON pm.purchase_id = p.id " +
            "JOIN users u ON pm.user_id = u.id";

    public static final String SQL_GET_ALL_USERS = "SELECT username FROM users";
}
