package de.untenrechts.urhome.transformation;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


public class PurchaseBuilder {

    public static JsonObject buildLightPurchase(final JsonObject purchase) {
        return new JsonObject()
                .put("id", purchase.getInteger("id"))
                .put("dateBought", purchase.getString("date_bought"))
                .put("productName", purchase.getString("product_name"))
                .put("price", purchase.getFloat("price"));
    }

    public static JsonObject buildFullPurchase(final JsonArray purchase) {
        return new JsonObject()
                .put("id", purchase.getLong(0))
                .put("market", purchase.getString(1))
                .put("dateBought", purchase.getValue(2))
                .put("productCategory", purchase.getString(3))
                .put("productName", purchase.getString(4))
                .put("price", purchase.getFloat(5))
                .put("buyer", purchase.getString(6))
                .put("consumptionMappings", new JsonArray(Buffer.buffer(purchase.getString(7))));
    }
}
