package de.untenrechts.urhome.database;

import de.untenrechts.urhome.transformation.PurchaseBuilder;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Collectors;

import static de.untenrechts.urhome.database.DatabaseQueries.*;

@Slf4j
public class AccountingDatabaseServiceImpl implements AccountingDatabaseService {

    private final JDBCClient jdbcClient;

    public AccountingDatabaseServiceImpl(final JDBCClient jdbcClient,
                                         Handler<AsyncResult<AccountingDatabaseService>> readyHandler) {
        this.jdbcClient = jdbcClient;
        testJdbcClient().onComplete(v -> {
            if (v.succeeded()) {
                readyHandler.handle(Future.succeededFuture(this));
            } else {
                readyHandler.handle(Future.failedFuture(v.cause()));
            }
        });
    }

    @Override
    public AccountingDatabaseService fetchAllUsers(Handler<AsyncResult<JsonArray>> resultHandler) {
        jdbcClient.query(SQL_GET_ALL_USERS, asyncFetch -> {
            if (asyncFetch.succeeded()) {
                final JsonArray users = new JsonArray(asyncFetch.result().getRows().stream()
                        .map(result -> result.getString("username"))
                        .collect(Collectors.toList()));

                resultHandler.handle(Future.succeededFuture(users));
            } else {
                log.error("Getting all users from database has failed.", asyncFetch.cause());
                resultHandler.handle(Future.failedFuture(asyncFetch.cause()));
            }
        });
        return this;
    }

    @Override
    public AccountingDatabaseService fetchPurchasesForUser(final String username, Handler<AsyncResult<JsonArray>> resultHandler) {
        verifyUser(username).onComplete(asyncVerification -> {
            if (asyncVerification.succeeded() && asyncVerification.result()) {
                jdbcClient.queryWithParams(SQL_GET_PURCHASES, new JsonArray().add(username), asyncFetch -> {
                    if (asyncFetch.succeeded()) {
                        final JsonArray result = new JsonArray(asyncFetch.result().getRows().stream()
                                .map(PurchaseBuilder::buildLightPurchase)
                                .collect(Collectors.toList()));
                        log.debug("Fetching purchases for username {} from database has succeeded", username);

                        resultHandler.handle(Future.succeededFuture(result));
                    } else {
                        log.error("Fetching purchases for username {} from database has failed.",
                                username, asyncFetch.cause());
                        resultHandler.handle(Future.failedFuture(asyncFetch.cause()));
                    }
                });
            } else if (!asyncVerification.result()) {
                log.warn("Fetching purchases for unknown username {} from database has failed.",
                        username);
                resultHandler.handle(Future.succeededFuture());
            } else {
                log.error("Fetching purchases for username {} from database has failed.",
                        username, asyncVerification.cause());
                resultHandler.handle(Future.failedFuture(asyncVerification.cause()));
            }
        });
        return this;
    }

    @Override
    public AccountingDatabaseService fetchPurchase(final long id, Handler<AsyncResult<JsonObject>> resultHandler) {
        jdbcClient.querySingleWithParams(SQL_GET_PURCHASE, new JsonArray().add(id).add(id), asyncFetch -> {
            if (asyncFetch.succeeded()) {
                log.debug("Fetching purchase for id {} from database has succeeded", id);
                if (asyncFetch.result() != null) {
                    final JsonObject result = PurchaseBuilder.buildFullPurchase(asyncFetch.result());
                    resultHandler.handle(Future.succeededFuture(result));
                } else {
                    log.warn("Fetching purchase for id {} from database has resulted in an empty response",
                            id);
                    resultHandler.handle(Future.succeededFuture());
                }
            } else {
                log.error("Fetching purchase for id {} from database has failed.", id,
                        asyncFetch.cause());
                resultHandler.handle(Future.failedFuture(asyncFetch.cause()));
            }
        });
        return this;
    }

    private Future<Boolean> verifyUser(final String username) {
        Promise<Boolean> promise = Promise.promise();
        jdbcClient.querySingleWithParams(SQL_VERIFY_USER, new JsonArray().add(username), asyncFetch -> {
            if (asyncFetch.succeeded()) {
                promise.complete(asyncFetch.result() != null);
            } else {
                promise.fail(asyncFetch.cause());
            }
        });
        return promise.future();
    }

    private Future<Void> testJdbcClient() {
        final Promise<Void> promise = Promise.promise();

        jdbcClient.getConnection(asyncConnection -> {
            if (asyncConnection.succeeded()) {
                final SQLConnection connection = asyncConnection.result();
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
                log.error("Establishing database connection has failed.", asyncConnection.cause());
                promise.fail(asyncConnection.cause());
            }
        });
        return promise.future();
    }
}
