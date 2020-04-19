package de.untenrechts.urhome;

import de.untenrechts.urhome.database.AccountingDatabaseVerticle;
import de.untenrechts.urhome.http.HttpServerVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class MainVerticle extends AbstractVerticle {

    public static String URHOME_DB_QUEUE = "urhome.db.queue";

    @Override
    public void start(Promise<Void> promise) {
        Promise<String> accountingDbVerticleDeployment = Promise.promise();
        vertx.deployVerticle(new AccountingDatabaseVerticle(),
                new DeploymentOptions().setConfig(config()),
                accountingDbVerticleDeployment);

        accountingDbVerticleDeployment.future().compose(id -> {
            Promise<String> httpVerticleDeployment = Promise.promise();
            vertx.deployVerticle(new HttpServerVerticle(),
                    new DeploymentOptions().setConfig(config()), httpVerticleDeployment);

            return httpVerticleDeployment.future();
        }).onComplete(asyncResult -> {
            if (asyncResult.succeeded()) {
                promise.complete();
            } else {
                promise.fail(asyncResult.cause());
            }
        });
    }
}
