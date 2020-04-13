package de.untenrechts.urhome.http;

import de.untenrechts.urhome.database.AccountingDatabaseService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.RequestParameter;
import io.vertx.ext.web.api.RequestParameters;
import io.vertx.ext.web.api.validation.CustomValidator;
import io.vertx.ext.web.api.validation.HTTPRequestValidationHandler;
import io.vertx.ext.web.api.validation.ParameterType;
import io.vertx.ext.web.api.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.List;

import static de.untenrechts.urhome.MainVerticle.URHOME_DB_QUEUE;


@Slf4j
public class HttpServerVerticle extends AbstractVerticle {

    private static final int PORT = 8080;

    private AccountingDatabaseService accountingDbService;

    @Override
    public void start(Promise<Void> promise) {
        accountingDbService = AccountingDatabaseService.createProxy(vertx, URHOME_DB_QUEUE);

        Router router = Router.router(vertx);
        router.get("/user/all").handler(this::fetchAllUsersHandler);
        router.get("/purchase/:id")
                .handler(fetchPurchaseValidationHandler())
                .handler(this::fetchPurchaseHandler);
        router.get("/purchase/all/:username")
                .handler(fetchPurchasesForUsernameValidationHandler())
                .handler(this::fetchPurchaseByUsernameHandler);
        router.post("/purchase")
                .handler(createPurchaseValidationHandler())
                .handler(this::createPurchase);
        router.put("/purchase/:id")
                .handler(updatePurchaseValidationHandler())
                .handler(this::updatePurchaseHandler);

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
    }

    private void createPurchase(RoutingContext ctx) {
        final RequestParameters params = ctx.get("parsedParameters");

        Iterator<RequestParameter> mappingUsernames
                = params.queryParameter("consumptionMappingUsernames").getArray().iterator();
        Iterator<RequestParameter> mappingShares
                = params.queryParameter("consumptionMappingShares").getArray().iterator();

        JsonObject consumptionMappings = new JsonObject();
        while (mappingUsernames.hasNext() && mappingShares.hasNext()) {
            consumptionMappings.put(
                    mappingUsernames.next().getString(),
                    mappingShares.next().getInteger()
            );
        }

        if (!mappingUsernames.hasNext() && !mappingShares.hasNext()) {
            accountingDbService.createPurchase(
                    params.queryParameter("buyer").getString(),
                    params.queryParameter("market").getString(),
                    params.queryParameter("dateBought").getString(),
                    params.queryParameter("productCategory").getString(),
                    params.queryParameter("productName").getString(),
                    params.queryParameter("price").getFloat(),
                    consumptionMappings,
                    asyncReply -> {
                        if (asyncReply.failed()) {
                            ctx.fail(asyncReply.cause());
                        } else if (!asyncReply.result()) {
                            ctx.response()
                                    .setStatusCode(404)
                                    .putHeader("Content-Type", "text/plain")
                                    .end("Username does not belong to any known user.");
                        } else {
                            ctx.response().end();
                        }
                    });
        } else {
            ctx.response()
                    .setStatusCode(400)
                    .putHeader("Content-Type", "text/plain")
                    .end("consumptionMappingUsernames and consumptionMappingShares must have " +
                            "the same number of entries.");
        }
    }

    private void fetchAllUsersHandler(RoutingContext ctx) {
        accountingDbService.fetchAllUsers(asyncReply -> {
            if (asyncReply.succeeded()) {
                ctx.response()
                        .putHeader("Content-Type", "application/json")
                        .end(asyncReply.result().toBuffer());
            } else {
                ctx.fail(asyncReply.cause());
            }
        });
    }

    private void fetchPurchaseByUsernameHandler(RoutingContext ctx) {
        final String username = ctx.request().getParam("username");
        accountingDbService.fetchPurchasesForUser(username, asyncReply -> {
            if (asyncReply.succeeded()) {
                final JsonArray result = asyncReply.result();
                if (result != null) {
                    ctx.response()
                            .putHeader("Content-Type", "application/json")
                            .end(asyncReply.result().toBuffer());
                } else {
                    ctx.response()
                            .setStatusCode(404)
                            .end();
                }
            } else {
                ctx.fail(asyncReply.cause());
            }
        });
    }

    private void fetchPurchaseHandler(RoutingContext ctx) {
        final int id = Integer.parseInt(ctx.request().getParam("id"));
        accountingDbService.fetchPurchase(id, asyncReply -> {
            if (asyncReply.succeeded()) {
                final JsonObject result = asyncReply.result();
                if (result != null) {
                    ctx.response()
                            .putHeader("Content-Type", "application/json")
                            .end(result.toBuffer());
                } else {
                    ctx.response()
                            .setStatusCode(404)
                            .end();
                }
            } else {
                ctx.fail(asyncReply.cause());
            }
        });
    }

    private void updatePurchaseHandler(RoutingContext ctx) {
        final RequestParameters params = ctx.get("parsedParameters");

        List<RequestParameter> mappingUsernames
                = params.queryParameter("consumptionMappingUsernames").getArray();
        List<RequestParameter> mappingShares
                = params.queryParameter("consumptionMappingShares").getArray();

        final boolean validMappingUpdate = mappingUsernames != null
                && mappingShares != null
                && mappingUsernames.size() == mappingShares.size();

        if (validMappingUpdate) {
            JsonObject consumptionMappings = new JsonObject();
            for (int i = 0; i <  mappingUsernames.size(); ++i) {
                consumptionMappings.put(mappingUsernames.get(i).getString(),
                        mappingShares.get(i).getFloat());
            }

            accountingDbService.updatePurchase(
                    params.pathParameter("id").getLong(),
                    params.queryParameter("buyer").getString(),
                    params.queryParameter("market").getString(),
                    params.queryParameter("dateBought").getString(),
                    params.queryParameter("productCategory").getString(),
                    params.queryParameter("productName").getString(),
                    params.queryParameter("price").getFloat(),
                    consumptionMappings,
                    asyncReply -> {
                        if (asyncReply.failed()) {
                            ctx.fail(asyncReply.cause());
                        } else if (!asyncReply.result()) {
                            ctx.response()
                                    .setStatusCode(404)
                                    .putHeader("Content-Type", "text/plain")
                                    .end("Username does not belong to any known user.");
                        } else {
                            ctx.response().end();
                        }
                    });
        } else {
            ctx.response()
                    .setStatusCode(400)
                    .putHeader("Content-Type", "text/plain")
                    .end("Either both query parameters (mappingUsernames, mappingShare - same " +
                            "length) or none of them must be present.");
        }
    }

    private static HTTPRequestValidationHandler createPurchaseValidationHandler() {
        return HTTPRequestValidationHandler.create()
                .addQueryParam("buyer", ParameterType.GENERIC_STRING, true)
                .addQueryParam("market", ParameterType.GENERIC_STRING, true)
                .addQueryParam("dateBought", ParameterType.DATE, true)
                .addQueryParam("productCategory", ParameterType.GENERIC_STRING, true)
                .addQueryParam("productName", ParameterType.GENERIC_STRING, true)
                .addQueryParam("price", ParameterType.FLOAT, true)
                .addQueryParamsArray("consumptionMappingUsernames", ParameterType.GENERIC_STRING, true)
                .addQueryParamsArray("consumptionMappingShares", ParameterType.INT, true);
    }

    private static HTTPRequestValidationHandler fetchPurchaseValidationHandler() {
        return HTTPRequestValidationHandler.create()
                .addPathParam("id", ParameterType.INT);
    }

    private static HTTPRequestValidationHandler fetchPurchasesForUsernameValidationHandler() {
        return HTTPRequestValidationHandler.create()
                .addPathParam("username", ParameterType.GENERIC_STRING);
    }

    private static HTTPRequestValidationHandler updatePurchaseValidationHandler() {
        return HTTPRequestValidationHandler.create()
                .addPathParam("id", ParameterType.INT)
                .addQueryParam("buyer", ParameterType.GENERIC_STRING, true)
                .addQueryParam("market", ParameterType.GENERIC_STRING, true)
                .addQueryParam("dateBought", ParameterType.GENERIC_STRING, true)
                .addQueryParam("productCategory", ParameterType.GENERIC_STRING, true)
                .addQueryParam("productName", ParameterType.GENERIC_STRING, true)
                .addQueryParam("price", ParameterType.FLOAT, true)
                .addQueryParamsArray("consumptionMappingUsernames", ParameterType.GENERIC_STRING, true)
                .addQueryParamsArray("consumptionMappingShare", ParameterType.FLOAT, true);
    }
}
