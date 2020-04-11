package de.untenrechts.urhome.transformation;

import io.vertx.core.json.JsonArray;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class UrhomeCollectors {

    // Credits: https://github.com/vert-x3/issues/issues/184#issuecomment-243155330
    public static <T> Collector<T, JsonArray, JsonArray> toJsonArray() {
        return new Collector<T, JsonArray, JsonArray>() {
            @Override
            public Supplier<JsonArray> supplier() {
                return JsonArray::new;
            }

            @Override
            public BiConsumer<JsonArray, T> accumulator() {
                return JsonArray::add;
            }

            @Override
            public BinaryOperator<JsonArray> combiner() {
                return JsonArray::addAll;
            }

            @Override
            public Function<JsonArray, JsonArray> finisher() {
                return Function.identity();
            }

            @Override
            public Set<Characteristics> characteristics() {
                return EnumSet.of(Characteristics.IDENTITY_FINISH);
            }
        };
    }
}
