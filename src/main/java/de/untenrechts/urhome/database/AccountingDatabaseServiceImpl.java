package de.untenrechts.urhome.database;

import de.untenrechts.urhome.transformation.PurchaseBuilder;
import de.untenrechts.urhome.transformation.UrhomeCollectors;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import lombok.extern.slf4j.Slf4j;

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
                final JsonArray users = asyncFetch.result().getRows().stream()
                        .map(result -> result.getString("username"))
                        .collect(UrhomeCollectors.toJsonArray());

                resultHandler.handle(Future.succeededFuture(users));
            } else {
                log.error("Getting all users from database has failed.", asyncFetch.cause());
                resultHandler.handle(Future.failedFuture(asyncFetch.cause()));
            }
        });
        return this;
    }

    @Override
    public AccountingDatabaseService fetchPurchase(long id, Handler<AsyncResult<JsonObject>> resultHandler) {
        fetchPurchaseById(id).onComplete(asyncPurchase -> {
            if (asyncPurchase.succeeded() && asyncPurchase.result() != null) {
                fetchPurchaseMappingsById(id).onComplete(asyncMappings -> {
                    if (asyncMappings.succeeded() && !asyncMappings.result().isEmpty()) {
                        final JsonObject result = PurchaseBuilder
                                .buildPurchase(asyncPurchase.result(), asyncMappings.result());

                        resultHandler.handle(Future.succeededFuture(result));
                    } else if (!asyncMappings.result().isEmpty()) {
                        resultHandler.handle(Future.succeededFuture(null));
                    } else {
                        resultHandler.handle(Future.failedFuture(asyncMappings.cause()));
                    }
                });
            } else if (asyncPurchase.result() == null) {
                resultHandler.handle(Future.succeededFuture(null));
            } else {
                resultHandler.handle(Future.failedFuture(asyncPurchase.cause()));
            }
        });
        return this;
    }

    private Future<JsonArray> fetchPurchaseById(long id) {
        Promise<JsonArray> promise = Promise.promise();
        jdbcClient.querySingleWithParams(SQL_GET_PURCHASE, new JsonArray().add(id), asyncFetch -> {
            if (asyncFetch.succeeded()) {
                log.debug("Fetching purchase for id {} form database has succeeded", id);
                promise.complete(asyncFetch.result());
            } else {
                log.error("Fetching purchase for id {} from database has failed.", id,
                        asyncFetch.cause());
                promise.fail(asyncFetch.cause());
            }
        });
        return promise.future();
    }

    private Future<JsonArray> fetchPurchaseMappingsById(long id) {
        Promise<JsonArray> promise = Promise.promise();
        jdbcClient.queryWithParams(SQL_GET_PURCHASE_MAPPINGS, new JsonArray().add(id), asyncFetch -> {
            if (asyncFetch.succeeded()) {
                log.debug("Fetching purchaseMappings for id {} has succeeded.", id);
                promise.complete(new JsonArray(asyncFetch.result().getResults()));
            } else {
                log.error("Fetching purchaseMappings for id {} has failed.", id, asyncFetch.cause());
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
