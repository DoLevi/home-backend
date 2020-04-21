package de.untenrechts.urhome.database;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.serviceproxy.ServiceBinder;
import lombok.extern.slf4j.Slf4j;

import static de.untenrechts.urhome.MainVerticle.URHOME_DB_QUEUE;
import static de.untenrechts.urhome.database.DatabaseConfig.*;


@Slf4j
public class AccountingDatabaseVerticle extends AbstractVerticle {

    private JDBCClient jdbcClient;

    @Override
    public void start(Promise<Void> promise) {
        final DatabaseConfig config = getDatabaseConfig();
        log.debug("Loading database config has succeeded.");

        jdbcClient = JDBCClient.createShared(vertx, new JsonObject()
                .put("url", config.getJdbcUrl())
                .put("driver_class", config.getDriverClass())
                .put("user", config.getUser())
                .put("password", config.getPassword()));

        AccountingDatabaseService.create(jdbcClient, asyncReady -> {
            if (asyncReady.succeeded()) {
                new ServiceBinder(vertx)
                        .setAddress(URHOME_DB_QUEUE)
                        .register(AccountingDatabaseService.class, asyncReady.result());

                promise.complete();
            } else {
                promise.fail(asyncReady.cause());
            }
        });
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        jdbcClient.close(v -> {
            if (v.succeeded()) {
                log.info("Successfully closed JDBC Client.");
            } else {
                log.error("Failed closing JDBC Client.", v.cause());
            }
            stopPromise.handle(v);
        });
    }

    private DatabaseConfig getDatabaseConfig() {
        return DatabaseConfig.builder()
                .jdbcUrl(config().getString(JDBC_URL_KEY))
                .driverClass(config().getString(DRIVER_CLASS_KEY))
                .user(config().getString(USER_KEY))
                .password(config().getString(PASSWORD_KEY))
                .build();
    }
}
