package de.untenrechts.urhome.transformation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
@Getter
public class Tuple <T, R> {

    private final T t;
    private final R r;
}
