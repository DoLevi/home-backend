package de.untenrechts.urhome.database;

public class DatabaseQueries {

    public static final String SQL_TEST_CONNECTION = "SELECT 1 " +
            "FROM purchase_mapping pm " +
            "JOIN purchases p ON pm.purchase_id = p.id " +
            "JOIN users u ON pm.user_id = u.id";
    public static final String SQL_GET_ALL_USERS = "SELECT username FROM users";
    public static final String SQL_GET_PURCHASES = "SELECT p.date_bought, p.product_name, p.price * m.consumption_share / s.share_sum AS price " +
            "FROM purchases p " +
            "JOIN purchase_mapping m ON p.id = m.purchase_id " +
            "JOIN users u ON m.user_id = u.id " +
            "JOIN (SELECT p.id, SUM(m.consumption_share) as share_sum " +
            "    FROM purchases p " +
            "    JOIN purchase_mapping m ON p.id = m.purchase_id " +
            "    GROUP BY p.id) s ON p.id = s.id " +
            "WHERE u.username = ?";
    public static final String SQL_GET_PURCHASE = "SELECT p.id, p.market, p.date_bought, p.product_category, p.product_name, p.price, u.username, json_agg(mapping_json) " +
            "FROM purchases p " +
            "JOIN users u ON p.buyer_user_id = u.id " +
            "JOIN (SELECT m.purchase_id, json_build_object('id', m.id, 'username', u.username, 'consumption_share', m.consumption_share) AS mapping_json " +
            "    FROM purchase_mapping m " +
            "    JOIN users u ON m.user_id = u.id " +
            "    WHERE m.purchase_id = ?) m ON p.id = m.purchase_id " +
            "WHERE p.id = ? " +
            "GROUP BY p.id, p.market, p.date_bought, p.product_category, p.product_name, p.price, u.username";
}
