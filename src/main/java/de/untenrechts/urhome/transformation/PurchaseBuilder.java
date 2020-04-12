package de.untenrechts.urhome.transformation;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


public class PurchaseBuilder {

    public static JsonObject buildPurchase(final JsonArray purchase, final JsonArray mappings) {

        return new JsonObject()
                .put("id", purchase.getLong(0))
                .put("market", purchase.getString(1))
                .put("dateBought", purchase.getValue(2))
                .put("productCategory", purchase.getString(3))
                .put("productName", purchase.getString(4))
                .put("price", purchase.getFloat(5))
                .put("buyer", purchase.getString(6))
                .put("consumptionMappings", buildPurchaseMappings(mappings));
    }

    private static JsonArray buildPurchaseMappings(final JsonArray mappings) {
        return mappings.stream()
                .map(mapping -> {
                    final JsonArray mappingRow = (JsonArray) mapping;
                    return new JsonObject()
                            .put("id", mappingRow.getLong(0))
                            .put("username", mappingRow.getString(1))
                            .put("share", mappingRow.getFloat(2));
                })
                .collect(UrhomeCollectors.toJsonArray());
    }
}
