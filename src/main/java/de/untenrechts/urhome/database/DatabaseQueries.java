package de.untenrechts.urhome.database;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@Slf4j
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

    private static String readOrRuntimeException(final String pathString) {
        final URL fileUrl = DatabaseQueries.class.getClassLoader().getResource(pathString);
        if (fileUrl != null) {
            try {
                return Files.readString(Path.of(fileUrl.toURI()));
            } catch (URISyntaxException | IOException e) {
                log.error("Unable to find SQL query file.", e);
                throw new IllegalStateException(e);
            }
        } else {
            final String message = String.format("File %s not found.", pathString);
            log.error(message);
            throw new IllegalStateException(message);
        }
    }
}
