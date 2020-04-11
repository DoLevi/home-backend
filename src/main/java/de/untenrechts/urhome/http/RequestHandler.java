package de.untenrechts.urhome.http;

import de.untenrechts.urhome.transformation.UrhomeCollectors;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static de.untenrechts.urhome.database.DatabaseQueries.SQL_GET_ALL_USERS;

@Slf4j
@RequiredArgsConstructor
public class RequestHandler {

    private final JDBCClient jdbcClient;

    public void userHandler(RoutingContext ctx) {
        jdbcClient.getConnection(asyncConnection -> {
            if (asyncConnection.succeeded()) {
                final SQLConnection connection = asyncConnection.result();

                connection.query(SQL_GET_ALL_USERS, asyncResult -> {
                    if (asyncResult.succeeded()) {
                        log.debug("Getting users has succeeded.");

                        ctx.response().putHeader("Content-Type", "application/json");
                        ctx.response().end(new JsonObject()
                                .put("usernames", resultSetToUsernames(asyncResult.result()))
                                .toBuffer());
                    } else {
                        log.error("Getting users has failed.", asyncResult.cause());
                    }
                });
            } else {
                log.error("Establishing database connection has failed.", asyncConnection.cause());
                ctx.fail(asyncConnection.cause());
            }
        });
    }

    private static JsonArray resultSetToUsernames(final ResultSet resultSet) {
        return resultSet.getRows().stream()
                .map(result -> result.getString("username"))
                .collect(UrhomeCollectors.toJsonArray());
    }
}
