package de.untenrechts.urhome.database;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;

@ProxyGen
public interface AccountingDatabaseService {

    @GenIgnore
    static AccountingDatabaseService create(final JDBCClient jdbcClient,
                                            Handler<AsyncResult<AccountingDatabaseService>> readyHandler) {
        return new AccountingDatabaseServiceImpl(jdbcClient, readyHandler);
    }

    @GenIgnore
    static AccountingDatabaseService createProxy(final Vertx vertx, final String address) {
        return new AccountingDatabaseServiceVertxEBProxy(vertx, address);
    }

    @Fluent
    AccountingDatabaseService fetchAllUsers(Handler<AsyncResult<JsonArray>> resultHandler);

    @Fluent
    AccountingDatabaseService fetchPurchase(long id, Handler<AsyncResult<JsonObject>> resultHandler);
}
