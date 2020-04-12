package de.untenrechts.urhome.database;

public class DatabaseQueries {

    public static final String SQL_TEST_CONNECTION = "SELECT 1 " +
            "FROM purchase_mapping pm " +
            "JOIN purchases p ON pm.purchase_id = p.id " +
            "JOIN users u ON pm.user_id = u.id";
    public static final String SQL_GET_ALL_USERS = "SELECT username FROM users";
    public static final String SQL_GET_PURCHASE = "SELECT p.id, p.market, p.date_bought, p.product_category, p.product_name, p.price, u.username " +
            "FROM purchases p " +
            "JOIN users u ON p.buyer_user_id = u.id " +
            "WHERE p.id = ?";
    public static final String SQL_GET_PURCHASE_MAPPINGS = "SELECT m.id, u.username, m.consumption_share " +
            "FROM purchase_mapping m " +
            "JOIN users u ON m.user_id = u.id " +
            "WHERE purchase_id = ?";
}
