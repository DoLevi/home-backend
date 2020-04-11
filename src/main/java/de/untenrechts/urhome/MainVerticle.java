package de.untenrechts.urhome;

import de.untenrechts.urhome.database.DatabaseConfig;
import de.untenrechts.urhome.http.RequestHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import lombok.extern.slf4j.Slf4j;

import static de.untenrechts.urhome.database.DatabaseConfig.*;
import static de.untenrechts.urhome.database.DatabaseQueries.SQL_TEST_CONNECTION;


@Slf4j
public class MainVerticle extends AbstractVerticle {

    private static final int PORT = 8080;

    private JDBCClient jdbcClient;
    private RequestHandler requestHandler;

    @Override
    public void start(Promise<Void> startPromise) {
        Future<Void> steps = prepareDatabase().compose(v -> startHttpServer());
        steps.onComplete(startPromise);
    }

    private Future<Void> prepareDatabase() {
        log.debug("Preparing database ...");
        final Promise<Void> promise = Promise.promise();

        final DatabaseConfig config = getDatabaseConfig();
        log.debug("Loading database config has succeeded.");

        jdbcClient = JDBCClient.createShared(vertx, new JsonObject()
                .put("url", config.getJdbcUrl())
                .put("driver_class", config.getDriverClass())
                .put("user", config.getUser())
                .put("password", config.getPassword()));

        testJdbcClient().onComplete(test -> {
            if (test.succeeded()) {
                requestHandler = new RequestHandler(jdbcClient);
                promise.complete();
            } else {
                promise.fail(test.cause());
            }
        });
        return promise.future();
    }

    private Future<Void> testJdbcClient() {
        final Promise<Void> promise = Promise.promise();

        jdbcClient.getConnection(ar -> {
            if (ar.succeeded()) {
                final SQLConnection connection = ar.result();
                log.debug("Establishing database connection has succeeded.");

                connection.query(SQL_TEST_CONNECTION, test -> {
                    connection.close();
                    if (test.succeeded()) {
                        log.info("Testing database connection has succeeded.");
                        promise.complete();
                    } else {
                        log.error("Testing database connection has failed.", test.cause());
                        promise.fail(test.cause());
                    }
                });
            } else {
                log.error("Establishing database connection has failed.", ar.cause());
                promise.fail(ar.cause());
            }
        });
        return promise.future();
    }

    private Future<Void> startHttpServer() {
        Promise<Void> promise = Promise.promise();

        Router router = Router.router(vertx);
        router.get("/user/all").handler(requestHandler::userHandler);

        HttpServer server = vertx.createHttpServer();
        server.requestHandler(router)
                .listen(PORT, ar -> {
                    if (ar.succeeded()) {
                        log.info("HTTP Server running on port {}.", PORT);
                        promise.complete();
                    } else {
                        log.error("Running HTTP Server on port {} has failed.", PORT, ar.cause());
                        promise.fail(ar.cause());
                    }
                });

        return promise.future();
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
