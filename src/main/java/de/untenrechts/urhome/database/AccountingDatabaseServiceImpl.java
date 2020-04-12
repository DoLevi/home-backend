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

import java.util.Iterator;
import java.util.Map;
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
    public AccountingDatabaseService createPurchase(final String buyer,
                                                    final String market,
                                                    final String dateBought,
                                                    final String productCategory,
                                                    final String productName,
                                                    final float price,
                                                    final JsonObject consumptionMappings,
                                                    Handler<AsyncResult<Boolean>> resultHandler) {
        fetchUserId(buyer).onComplete(asyncBuyerId -> {
            if (!asyncBuyerId.succeeded()) {
                log.error("Creating purchase in database has failed.", asyncBuyerId.cause());
                resultHandler.handle(Future.failedFuture(asyncBuyerId.cause()));
                return;
            }
            log.debug("Fetching all user-ids from database for user {} has succeeded.", buyer);

            if (asyncBuyerId.result() == null) {
                log.warn("Creating purchase for unknown buyer {} in database has failed.", buyer);
                resultHandler.handle(Future.succeededFuture(false));
                return;
            }
            final long buyerId = asyncBuyerId.result();
            log.debug("Fetching a valid user-id from database for user {} has succeeded.", buyer);

            jdbcClient.getConnection(asyncConnection -> {
                if (!asyncConnection.succeeded()) {
                    log.error("Establishing a database connection has failed.",
                            asyncConnection.cause());
                    resultHandler.handle(Future.failedFuture(asyncConnection.cause()));
                    return;
                }
                log.debug("Establishing a database connection has succeeded.");

                final SQLConnection connection = asyncConnection.result();
                connection.setAutoCommit(false, asyncSetter -> {
                    if (!asyncSetter.succeeded()) {
                        log.error("Establishing a database connection has failed.",
                                asyncConnection.cause());
                        resultHandler.handle(Future.failedFuture(asyncConnection.cause()));
                        return;
                    }
                    log.debug("Setting autoCommit to false for this connection has succeeded.");

                    createPurchase(connection, buyerId, market, dateBought, productCategory, productName, price, consumptionMappings, resultHandler);
                });
            });
        });
        return this;
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
        fetchUserId(username).onComplete(asyncVerification -> {
            if (asyncVerification.succeeded() && asyncVerification.result() != null) {
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
            } else if (asyncVerification.result() == null) {
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

    private static void createPurchase(final SQLConnection connection,
                                       final long buyerId,
                                       final String market,
                                       final String dateBought,
                                       final String productCategory,
                                       final String productName,
                                       final float price,
                                       final JsonObject consumptionMappings,
                                       Handler<AsyncResult<Boolean>> resultHandler) {
        connection.updateWithParams(SQL_CREATE_PURCHASE,
                new JsonArray()
                        .add(market)
                        .add(dateBought)
                        .add(productCategory)
                        .add(productName)
                        .add(price)
                        .add(buyerId),
                asyncUpdate -> {
                    if (asyncUpdate.succeeded()) {
                        final long insertedPurchaseId = asyncUpdate.result()
                                .getKeys()
                                .getLong(0);
                        createPurchaseMappings(connection, insertedPurchaseId, consumptionMappings)
                                .onSuccess(allKnown -> resultHandler.handle(Future.succeededFuture(allKnown)))
                                .onFailure(t -> connection.rollback(v -> {
                                    if (v.succeeded()) {
                                        resultHandler.handle(Future.failedFuture(t));
                                    } else {
                                        log.error("Rolling back for creation of " +
                                                "purchaseMappings has failed.", v.cause());
                                        resultHandler.handle(Future.failedFuture(v.cause()));
                                    }
                                }));
                    } else {
                        connection.close();
                        log.error("Creating purchase in database has failed.",
                                asyncUpdate.cause());
                        resultHandler.handle(Future.failedFuture(asyncUpdate.cause()));
                    }
                });
    }

    private static Future<Boolean> createPurchaseMappings(final SQLConnection connection,
                                                          final long purchaseId,
                                                          final JsonObject consumptionMappings) {
        Promise<Boolean> promise = Promise.promise();

        Iterator<Map.Entry<String, Object>> iterator = consumptionMappings.iterator();
        createPurchaseMappings(connection, purchaseId, iterator, asyncResult -> {
            if (asyncResult.failed()) {
                promise.fail(asyncResult.cause());
            } else if (asyncResult.result()) {
                connection.commit(v -> promise.complete(true));
            } else {
                promise.complete(false);
            }
        });

        return promise.future();
    }

    private static void createPurchaseMappings(final SQLConnection connection,
                                               final long purchaseId,
                                               Iterator<Map.Entry<String, Object>> remainingConsumptionMappings,
                                               Handler<AsyncResult<Boolean>> resultHandler) {
        if (remainingConsumptionMappings.hasNext()) {
            final Map.Entry<String, Object> entry = remainingConsumptionMappings.next();
            fetchUserId(connection, entry.getKey())
                    .onSuccess(buyerId -> {
                        if (buyerId != null) {
                            connection.updateWithParams(SQL_CREATE_PURCHASE_MAPPINGS,
                                    new JsonArray()
                                            .add(purchaseId)
                                            .add(buyerId)
                                            .add(entry.getValue()),
                                    asyncUpdate -> {
                                        if (asyncUpdate.succeeded()) {
                                            log.debug("Creating purchase mapping for purchase id {} and consumer {} has succeeded.",
                                                    purchaseId, entry.getKey());
                                            createPurchaseMappings(connection, purchaseId,
                                                    remainingConsumptionMappings, resultHandler);
                                        } else {
                                            resultHandler.handle(Future.failedFuture(asyncUpdate.cause()));
                                        }
                                    });
                        } else {
                            log.error("Creating purchase mapping for purchase id {} unknown consumer {} has failed.",
                                    purchaseId, entry.getKey());
                            resultHandler.handle(Future.succeededFuture(false));
                        }
                    })
                    .onFailure(throwable -> resultHandler.handle(Future.failedFuture(throwable)));
        } else {
            log.info("Leaving last iteration");
            resultHandler.handle(Future.succeededFuture(true));
        }
    }

    private Future<Long> fetchUserId(final String username) {
        Promise<Long> promise = Promise.promise();
        jdbcClient.getConnection(asyncConnection -> {
            if (asyncConnection.succeeded()) {
                fetchUserId(asyncConnection.result(), username)
                        .onSuccess(promise::complete)
                        .onFailure(promise::fail);
            } else {
                promise.fail(asyncConnection.cause());
            }
        });
        return promise.future();
    }

    private static Future<Long> fetchUserId(final SQLConnection connection, final String username) {
        Promise<Long> promise = Promise.promise();
        connection.querySingleWithParams(SQL_GET_USER_ID, new JsonArray().add(username), asyncFetch -> {
            if (asyncFetch.succeeded() && asyncFetch.result() != null) {
                promise.complete(asyncFetch.result().getLong(0));
            } else if (asyncFetch.result() == null) {
                promise.complete(null);
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
