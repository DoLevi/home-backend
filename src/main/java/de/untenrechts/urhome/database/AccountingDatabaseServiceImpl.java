package de.untenrechts.urhome.database;

import de.untenrechts.urhome.transformation.PurchaseBuilder;
import de.untenrechts.urhome.transformation.Tuple;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        jdbcClient.getConnection(asyncConnection -> {
            if (asyncConnection.succeeded()) {
                // Prepare connection
                final SQLConnection connection = asyncConnection.result();
                log.debug("Disabling autocommit for purchase creation, buyer {}...", buyer);
                connection.setAutoCommit(false, v -> {
                    if (v.failed()) {
                        log.error("Disabling autocommit for purchase creation, buyer {} failed.",
                                buyer);
                        resultHandler.handle(Future.failedFuture(v.cause()));
                    }
                });

                final Map<String, Integer> consumptionMappingsMap = consumptionMappings.stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> (Integer) entry.getValue()));

                // Create purchase
                Future<Boolean> createPurchaseSteps = fetchUserId(connection, buyer)
                        .compose(buyerId -> createPurchase(connection, buyerId, market, dateBought, productCategory, productName, price))
                        .compose(purchaseId -> extractConsumptionMappings(connection, purchaseId, Stream.empty(), consumptionMappingsMap.entrySet().iterator()))
                        .compose(mappings -> createConsumptionMappings(connection, mappings.getT(), mappings.getR()));

                // Clean everything up
                createPurchaseSteps
                        .onSuccess(bUpdate -> {
                            if (bUpdate) {
                                log.debug("Committing purchase creation, buyer {}...", buyer);
                                connection.commit(vCommit -> {
                                    connection.close();
                                    if (vCommit.failed()) {
                                        log.error("Committing for purchase creation, buyer {} " +
                                                "failed.", buyer, vCommit.cause());
                                        resultHandler.handle(Future.failedFuture(vCommit.cause()));
                                    } else {
                                        resultHandler.handle(Future.succeededFuture(true));
                                    }
                                });
                            } else {
                                log.error("Creating purchase, buyer {} failed.", buyer);
                                connection.close();
                                resultHandler.handle(Future.succeededFuture(false));
                            }
                        });
            } else {
                log.error("Establishing database connection for purchase creation, buyer {} failed.",
                        buyer, asyncConnection.cause());
                resultHandler.handle(Future.failedFuture(asyncConnection.cause()));
            }
        });
        return this;
    }

    private static Future<Long> createPurchase(final SQLConnection connection,
                                               final Long buyerId,
                                               final String market,
                                               final String dateBought,
                                               final String productCategory,
                                               final String productName,
                                               final float price) {
        Promise<Long> promise = Promise.promise();
        if (buyerId != null) {
            final JsonArray queryParams = new JsonArray()
                    .add(market)
                    .add(dateBought)
                    .add(productCategory)
                    .add(productName)
                    .add(price)
                    .add(buyerId);
            log.debug("Creating purchase, buyer id {}...", buyerId);

            connection.updateWithParams(SQL_CREATE_PURCHASE, queryParams, asyncUpdate -> {
                if (asyncUpdate.succeeded()) {
                    final long purchaseId = asyncUpdate.result().getKeys().getLong(0);
                    promise.complete(purchaseId);
                } else {
                    log.error("Creating purchase, buyer id {} failed.", buyerId,
                            asyncUpdate.cause());
                    promise.fail(asyncUpdate.cause());
                }
            });
        } else {
            promise.complete();
        }
        return promise.future();
    }

    private static Future<Tuple<Long, Map<Long, Integer>>> extractConsumptionMappings(final SQLConnection connection,
                                                                                      final Long purchaseId,
                                                                                      Stream<Map.Entry<Long, Integer>> extractedMappings,
                                                                                      final Iterator<Map.Entry<String, Integer>> rawMappings) {
        Promise<Tuple<Long, Map<Long, Integer>>> promise = Promise.promise();
        if (purchaseId != null) {
            if (rawMappings.hasNext()) {
                final Map.Entry<String, Integer> rawMapping = rawMappings.next();
                log.debug("Extracting consumption mapping, purchase id {}, username {}...",
                        purchaseId, rawMapping.getKey());

                fetchUserId(connection, rawMapping.getKey())
                        .onSuccess(consumerId -> {
                            log.debug("Building consumption mapping entry, purchase id {}, " +
                                    "user id {}...", purchaseId, consumerId);
                            final Map.Entry<Long, Integer> mapping
                                    = Map.entry(consumerId, rawMapping.getValue());

                            extractConsumptionMappings(connection, purchaseId,
                                    Stream.concat(extractedMappings, Stream.of(mapping)), rawMappings)
                                    .onSuccess(promise::complete)
                                    .onFailure(promise::fail);
                        })
                        .onFailure(promise::fail);
            } else {
                log.debug("Building Map.Entry from extracted consumption mapping data...");
                final Map<Long, Integer> result = extractedMappings
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                promise.complete(new Tuple<>(purchaseId, result));
            }
        } else {
            promise.complete();
        }
        return promise.future();
    }

    private static Future<Boolean> createConsumptionMappings(final SQLConnection connection,
                                                             final Long purchaseId,
                                                             final Map<Long, Integer> mappings) {
        Promise<Boolean> promise = Promise.promise();
        if (purchaseId != null) {
            log.debug("Creating consumption mappings, purchase id {}...", purchaseId);
            createConsumptionMappings(connection, purchaseId, mappings.entrySet().iterator())
                    .onSuccess(v -> promise.complete(true))
                    .onFailure(promise::fail);
        } else {
            promise.complete(false);
        }
        return promise.future();
    }

    @Override
    public AccountingDatabaseService fetchAllUsers(Handler<AsyncResult<JsonArray>> resultHandler) {
        log.debug("Fetching all users...");
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
    public AccountingDatabaseService fetchPurchasesForUser(final String username, Handler<AsyncResult<JsonObject>> resultHandler) {
        jdbcClient.getConnection(asyncConnection -> {
            if (asyncConnection.succeeded()) {
                final SQLConnection connection = asyncConnection.result();
                fetchUserId(connection, username)
                        .compose(userId -> fetchLightPurchasesForUser(connection, userId))
                        .compose(purchases -> {
                            Promise<JsonObject> promise = Promise.promise();
                            fetchDeltas(connection, username)
                                    .onSuccess(deltas -> promise.complete(new JsonObject()
                                            .put("purchases", purchases)
                                            .put("summary", deltas)))
                                    .onFailure(promise::fail);
                            return promise.future();
                        })
                        .onComplete(resultHandler);
            } else {
                resultHandler.handle(Future.failedFuture(asyncConnection.cause()));
            }
        });
        return this;
    }

    private static Future<JsonArray> fetchLightPurchasesForUser(final SQLConnection connection,
                                                                final Long userId) {
        Promise<JsonArray> promise = Promise.promise();
        if (userId != null) {
            final JsonArray queryParams = new JsonArray().add(userId);
            log.debug("Fetching purchases for user, id {}...", userId);

            connection.queryWithParams(SQL_GET_PURCHASES, queryParams, asyncFetch -> {
                if (asyncFetch.succeeded()) {
                    final JsonArray purchases = new JsonArray(asyncFetch.result().getRows().stream()
                            .map(PurchaseBuilder::buildLightPurchase)
                            .collect(Collectors.toList()));
                    promise.complete(purchases);
                } else {
                    log.error("Fetching purchases for user, id {} failed.", userId);
                    promise.fail(asyncFetch.cause());
                }
            });
        } else {
            promise.complete(null);
        }
        return promise.future();
    }

    private static Future<JsonObject> fetchDeltas(final SQLConnection connection,
                                                  final String user) {
        Promise<JsonObject> promise = Promise.promise();

        final JsonArray queryParams = new JsonArray().add(user).add(user);

        connection.queryWithParams(SQL_GET_DELTAS, queryParams, asyncFetch -> {
            if (asyncFetch.succeeded()) {
                final Map<String, Object> debtsAndClaims = asyncFetch.result().getRows().stream()
                        .reduce(new HashMap<>(),
                                AccountingDatabaseServiceImpl::reduceDeltaRow,
                                AccountingDatabaseServiceImpl::mergeDeltaRows)
                        .entrySet().stream()
                        .collect(Collectors.toMap(entry
                                -> Objects.toString(entry.getKey()), Map.Entry::getValue));

                promise.complete(new JsonObject(debtsAndClaims));
            } else {
                log.error("Fetching expense deltas, user {} failed.", user, asyncFetch.cause());
                promise.fail(asyncFetch.cause());
            }
        });
        return promise.future();
    }

    private static HashMap<String, Float> reduceDeltaRow(final HashMap<String, Float> previous,
                                                         final JsonObject current) {
        final String buyer = current.getString("buyer");
        final String consumer = current.getString("consumer");
        final float delta = current.getFloat("delta");

        previous.compute(buyer, (claimerId, claim) -> claim == null ? - delta : claim - delta);
        previous.compute(consumer, (key, debt) -> debt == null ? delta : debt + delta);

        return previous;
    }

    private static HashMap<String, Float> mergeDeltaRows(final HashMap<String, Float> map1,
                                                         final HashMap<String, Float> map2) {
        for (Map.Entry<String, Float> entry : map2.entrySet()) {
            map1.compute(entry.getKey(),
                    (key, delta) -> delta == null ? entry.getValue() : delta + entry.getValue());
        }
        return map1;
    }

    @Override
    public AccountingDatabaseService fetchPurchase(final long id, Handler<AsyncResult<JsonObject>> resultHandler) {
        log.debug("Fetching purchase, id {}...", id);
        final JsonArray queryParams = new JsonArray().add(id).add(id);

        jdbcClient.querySingleWithParams(SQL_GET_PURCHASE, queryParams, asyncFetch -> {
            if (asyncFetch.succeeded()) {
                if (asyncFetch.result() != null) {
                    final JsonObject result = PurchaseBuilder.buildFullPurchase(asyncFetch.result());
                    resultHandler.handle(Future.succeededFuture(result));
                } else {
                    log.warn("Fetching purchase, id {} yielded no matching results.", id);
                    resultHandler.handle(Future.succeededFuture());
                }
            } else {
                log.error("Fetching purchase, id {} failed.", id, asyncFetch.cause());
                resultHandler.handle(Future.failedFuture(asyncFetch.cause()));
            }
        });
        return this;
    }

    @Override
    public AccountingDatabaseService updatePurchase(long id,
                                                    final String buyer,
                                                    final String market,
                                                    final String dateBought,
                                                    final String productCategory,
                                                    final String productName,
                                                    final Float price,
                                                    final JsonObject consumptionMappings,
                                                    Handler<AsyncResult<Boolean>> resultHandler) {
        log.debug("Establishing database connection for purchase update, id {}...", id);
        jdbcClient.getConnection(asyncConnection -> {
            if (asyncConnection.succeeded()) {
                // Prepare connection
                final SQLConnection connection = asyncConnection.result();
                log.debug("Disabling autocommit for purchase update, id {}...", id);
                connection.setAutoCommit(false, v -> {
                    if (v.failed()) {
                        log.error("Disabling autocommit for purchase update, id {} failed.", id);
                        resultHandler.handle(Future.failedFuture(v.cause()));
                    }
                });

                // Execute the update
                Future<Void> updatePurchaseSteps = fetchUserId(connection, buyer)
                        .compose(buyerId -> updatePurchase(connection, id, buyerId, market, dateBought, productCategory, productName, price))
                        .compose(vUpdatePurchase -> updateConsumptionMappings(connection, id, consumptionMappings));

                log.debug("Committing and closing connection for purchase update, id {}...", id);

                // Clean everything up
                updatePurchaseSteps
                        .onSuccess(vUpdate -> connection.commit(vCommit -> {
                            connection.close();
                            if (vCommit.failed()) {
                                log.error("Committing for purchase update, id {} failed.",
                                        id, vCommit.cause());
                                resultHandler.handle(Future.failedFuture(vCommit.cause()));
                            } else {
                                resultHandler.handle(Future.succeededFuture(true));
                            }
                        }))
                        .onFailure(thrown -> {
                            connection.close();
                            log.error("Updating purchase, id {} failed.", id, thrown);
                            resultHandler.handle(Future.failedFuture(thrown));
                        });
            } else {
                log.error("Establishing database connection for purchase update, id {} failed.",
                        id, asyncConnection.cause());
                resultHandler.handle(Future.failedFuture(asyncConnection.cause()));
            }
        });
        return this;
    }

    private static Future<Void> updatePurchase(final SQLConnection connection,
                                               final long purchaseId,
                                               final long buyerId,
                                               final String market,
                                               final String dateBought,
                                               final String productCategory,
                                               final String productName,
                                               final Float price) {
        Promise<Void> promise = Promise.promise();
        final JsonArray sqlQueryParams = new JsonArray()
                .add(market)
                .add(dateBought)
                .add(productCategory)
                .add(productName)
                .add(price)
                .add(buyerId)
                .add(purchaseId);

        log.debug("Updating purchase with id {}...", purchaseId);

        connection.updateWithParams(SQL_UPDATE_PURCHASE, sqlQueryParams, asyncUpdate -> {
            if (asyncUpdate.succeeded()) {
                promise.complete();
            } else {
                log.error("Updating purchase with id {} failed.", purchaseId, asyncUpdate.cause());
                promise.fail(asyncUpdate.cause());
            }
        });
        return promise.future();
    }

    private static Future<Void> updateConsumptionMappings(final SQLConnection connection,
                                                          final long purchaseId,
                                                          final JsonObject mappings) {
        Promise<Void> promise = Promise.promise();

        extractConsumptionMappings(connection, purchaseId, mappings)
                .compose(desiredMappings -> {
                    Promise<Tuple<Map<Long, Integer>, Map<Long, Integer>>> mapsPromise
                            = Promise.promise();

                    fetchConsumptionMappings(connection, purchaseId)
                            .onSuccess(knownMappings -> mapsPromise.complete(
                                    new Tuple<>(knownMappings, desiredMappings)))
                            .onFailure(mapsPromise::fail);

                    return mapsPromise.future();
                })
                .onSuccess(mappingsTuple -> {
                    final Map<Long, Integer> knownMappings = mappingsTuple.getT();
                    final Map<Long, Integer> desiredMappings = mappingsTuple.getR();

                    final Iterator<Map.Entry<Long, Integer>> missingMappings
                            = desiredMappings.entrySet().stream()
                            .filter(entry -> !knownMappings.containsKey(entry.getKey()))
                            .iterator();

                    final Iterator<Map.Entry<Long, Integer>> changedMappings
                            = desiredMappings.entrySet().stream()
                            .filter(entry -> knownMappings.containsKey(entry.getKey()))
                            .iterator();

                    final Iterator<Long> superfluousMappings
                            = knownMappings.keySet().stream()
                            .filter(buyerId -> !desiredMappings.containsKey(buyerId))
                            .iterator();

                    createConsumptionMappings(connection, purchaseId, missingMappings)
                            .compose(v -> updateConsumptionMappings(connection, purchaseId, changedMappings))
                            .compose(v -> removeConsumptionMappings(connection, purchaseId, superfluousMappings))
                            .onComplete(promise);
                })
                .onFailure(promise::fail);

        return promise.future();
    }

    private static Future<Map<Long, Integer>> extractConsumptionMappings(final SQLConnection connection,
                                                                         final long purchaseId,
                                                                         final JsonObject mappings) {
        Promise<Map<Long, Integer>> mappingPromise = Promise.promise();
        Iterator<Map.Entry<String, Integer>> iterator = mappings.stream()
                .map(entry -> Map.entry(entry.getKey(), (Integer) entry.getValue()))
                .iterator();
        extractConsumptionMappings(connection, purchaseId, Stream.empty(), iterator)
                .onComplete(asyncEntry -> {
                    if (asyncEntry.succeeded()) {
                        mappingPromise.complete(asyncEntry.result().getR());
                    } else {
                        mappingPromise.fail(asyncEntry.cause());
                    }
                });
        return mappingPromise.future();
    }

    private static Future<Map<Long, Integer>> fetchConsumptionMappings(final SQLConnection connection,
                                                                       final long purchaseId) {
        Promise<Map<Long, Integer>> promise = Promise.promise();
        connection.queryWithParams(SQL_GET_PURCHASE_MAPPINGS, new JsonArray().add(purchaseId),
                asyncMappings -> {
                    if (asyncMappings.succeeded()) {
                        final Map<Long, Integer> mappings = asyncMappings.result().getResults()
                                .stream()
                                .collect(Collectors.toMap(
                                        element -> element.getLong(0),
                                        element -> element.getInteger(1)
                                ));
                        log.debug("Fetching purchase mappings for purchase with id {} succeeded.",
                                purchaseId);

                        promise.complete(mappings);
                    } else {
                        log.error("Fetching purchase mappings for purchase with id {} failed.",
                                purchaseId, asyncMappings.cause());
                        promise.fail(asyncMappings.cause());
                    }
                });
        return promise.future();
    }

    private static Future<Void> createConsumptionMappings(final SQLConnection connection,
                                                          final long purchaseId,
                                                          Iterator<Map.Entry<Long, Integer>> missingMappings) {
        Promise<Void> promise = Promise.promise();
        if (missingMappings.hasNext()) {
            final Map.Entry<Long, Integer> newMapping = missingMappings.next();
            final JsonArray queryParams = new JsonArray()
                    .add(purchaseId)
                    .add(newMapping.getKey())
                    .add(newMapping.getValue());

            log.debug("Creating consumption mapping for purchase with id {} and user with id {}...",
                    purchaseId, newMapping.getKey());

            connection.updateWithParams(SQL_CREATE_PURCHASE_MAPPING, queryParams, asyncUpdate -> {
                if (asyncUpdate.succeeded()) {
                    createConsumptionMappings(connection, purchaseId, missingMappings)
                            .onComplete(promise);
                } else {
                    log.error("Creating consumption mapping for purchase with id {} failed.",
                            purchaseId, asyncUpdate.cause());
                    promise.fail(asyncUpdate.cause());
                }
            });
        } else {
            promise.complete();
        }
        return promise.future();
    }

    private static Future<Void> updateConsumptionMappings(final SQLConnection connection,
                                                          final long purchaseId,
                                                          Iterator<Map.Entry<Long, Integer>> changedMappings) {
        Promise<Void> promise = Promise.promise();
        if (changedMappings.hasNext()) {
            final Map.Entry<Long, Integer> newMapping = changedMappings.next();
            final JsonArray queryParams = new JsonArray()
                    .add(newMapping.getValue())
                    .add(purchaseId)
                    .add(newMapping.getKey());

            log.debug("Updating consumption mapping for purchase with id {} and user with id {}...",
                    purchaseId, newMapping.getKey());

            connection.updateWithParams(SQL_UPDATE_PURCHASE_MAPPING, queryParams, asyncUpdate -> {
                if (asyncUpdate.succeeded()) {
                    updateConsumptionMappings(connection, purchaseId, changedMappings)
                            .onComplete(promise);
                } else {
                    log.error("Updating consumption mapping for purchase with id {} failed.",
                            purchaseId, asyncUpdate.cause());
                    promise.fail(asyncUpdate.cause());
                }
            });
        } else {
            promise.complete();
        }
        return promise.future();
    }

    private static Future<Void> removeConsumptionMappings(final SQLConnection connection,
                                                          final long purchaseId,
                                                          Iterator<Long> superfluousBuyerIds) {
        Promise<Void> promise = Promise.promise();
        if (superfluousBuyerIds.hasNext()) {
            final long buyerId = superfluousBuyerIds.next();
            final JsonArray queryParams = new JsonArray()
                    .add(purchaseId)
                    .add(buyerId);

            log.debug("Removing consumption mapping for purchase with id {} and user with id {}...",
                    purchaseId, buyerId);

            connection.updateWithParams(SQL_DELETE_PURCHASE_MAPPING, queryParams, asyncUpdate -> {
                if (asyncUpdate.succeeded()) {
                    removeConsumptionMappings(connection, purchaseId, superfluousBuyerIds)
                            .onComplete(promise);
                } else {
                    log.error("Removing consumption mapping for purchase with id {} failed.",
                            purchaseId, asyncUpdate.cause());
                    promise.fail(asyncUpdate.cause());
                }
            });
        } else {
            promise.complete();
        }
        return promise.future();
    }

    private static Future<Long> fetchUserId(final SQLConnection connection, final String username) {
        Promise<Long> promise = Promise.promise();
        connection.querySingleWithParams(SQL_GET_USER_ID, new JsonArray().add(username), asyncFetch -> {
            if (asyncFetch.succeeded() && asyncFetch.result() != null) {
                promise.complete(asyncFetch.result().getLong(0));
            } else if (asyncFetch.result() == null) {
                log.warn("Fetching user, name {} yielded no matching results.", username);
                promise.complete(null);
            } else {
                log.error("Fetching user, name {} failed.", username, asyncFetch.cause());
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
