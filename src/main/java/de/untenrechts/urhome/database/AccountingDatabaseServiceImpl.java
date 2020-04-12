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
        fetchPurchasesByUsername(username).onComplete(resultHandler);
        return this;
    }

    @Override
    public AccountingDatabaseService fetchPurchase(final long id, Handler<AsyncResult<JsonObject>> resultHandler) {
        fetchPurchaseById(id).onComplete(asyncPurchase -> {
            if (asyncPurchase.succeeded() && asyncPurchase.result() != null) {
                final JsonObject result = PurchaseBuilder.buildFullPurchase(asyncPurchase.result());
                resultHandler.handle(Future.succeededFuture(result));
            } else if (asyncPurchase.result() == null) {
                resultHandler.handle(Future.succeededFuture(null));
            } else {
                resultHandler.handle(Future.failedFuture(asyncPurchase.cause()));
            }
        });
        return this;
    }

    private Future<JsonArray> fetchPurchasesByUsername(final String username) {
        Promise<JsonArray> promise = Promise.promise();
        jdbcClient.queryWithParams(SQL_GET_PURCHASES, new JsonArray().add(username), asyncFetch -> {
            if (asyncFetch.succeeded()) {
                log.debug("Fetching purchases for username {} from database has succeeded", username);
                promise.complete(new JsonArray(asyncFetch.result().getResults()));
            } else {
                log.error("Fetching purchases for username {} from database has failed.",
                        username, asyncFetch.cause());
                promise.fail(asyncFetch.cause());
            }
        });
        return promise.future();
    }

    private Future<JsonArray> fetchPurchaseById(final long id) {
        Promise<JsonArray> promise = Promise.promise();
        jdbcClient.querySingleWithParams(SQL_GET_PURCHASE, new JsonArray().add(id).add(id), asyncFetch -> {
            if (asyncFetch.succeeded()) {
                log.debug("Fetching purchase for id {} from database has succeeded", id);
                promise.complete(asyncFetch.result());
            } else {
                log.error("Fetching purchase for id {} from database has failed.", id,
                        asyncFetch.cause());
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
