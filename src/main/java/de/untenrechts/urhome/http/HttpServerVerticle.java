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
import io.vertx.ext.web.api.validation.HTTPRequestValidationHandler;
import io.vertx.ext.web.api.validation.ParameterType;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;

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
                .handler(HttpServerVerticle.getPurchaseValidationHandler())
                .handler(this::fetchPurchaseHandler);
        router.get("/purchase/all/:username")
                .handler(HttpServerVerticle.getPurchasesForUsernameValidationHandler())
                .handler(this::fetchPurchaseByUsernameHandler);
        router.post("/purchase")
                .handler(HttpServerVerticle.createPurchaseValidationHandler())
                .handler(this::createPurchase);

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
                             ctx.response()
                                    .putHeader("Content-Type", "application/json")
                                    .end();
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
                if (result!= null) {
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

    private static HTTPRequestValidationHandler getPurchaseValidationHandler() {
        return HTTPRequestValidationHandler.create()
                .addPathParam("id", ParameterType.INT);
    }

    private static HTTPRequestValidationHandler getPurchasesForUsernameValidationHandler() {
        return HTTPRequestValidationHandler.create()
                .addPathParam("username", ParameterType.GENERIC_STRING);
    }
}
