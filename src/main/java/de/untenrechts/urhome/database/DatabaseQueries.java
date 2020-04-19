package de.untenrechts.urhome.database;

import static de.untenrechts.urhome.Configuration.readOrRuntimeException;


public class DatabaseQueries {

    public static final String SQL_TEST_CONNECTION = readOrRuntimeException("sql/test_connection.sql");

    public static final String SQL_GET_ALL_USERS = readOrRuntimeException("sql/get_all_users.sql");
    public static final String SQL_GET_USER_ID = readOrRuntimeException("sql/get_user_id.sql");

    public static final String SQL_CREATE_PURCHASE = readOrRuntimeException("sql/create_purchase.sql");
    public static final String SQL_GET_PURCHASES = readOrRuntimeException("sql/get_purchases.sql");
    public static final String SQL_GET_PURCHASE = readOrRuntimeException("sql/get_purchase.sql");
    public static final String SQL_GET_DELTAS = readOrRuntimeException("sql/get_deltas.sql");

    public static final String SQL_CREATE_PURCHASE_MAPPING = readOrRuntimeException("sql/create_purchase_mapping.sql");
    public static final String SQL_GET_PURCHASE_MAPPINGS = readOrRuntimeException("sql/get_purchase_mappings.sql");
    public static final String SQL_UPDATE_PURCHASE = readOrRuntimeException("sql/update_purchase.sql");
    public static final String SQL_UPDATE_PURCHASE_MAPPING = readOrRuntimeException("sql/update_purchase_mapping.sql");
    public static final String SQL_DELETE_PURCHASE_MAPPING = readOrRuntimeException("sql/delete_purchase_mapping.sql");
}
