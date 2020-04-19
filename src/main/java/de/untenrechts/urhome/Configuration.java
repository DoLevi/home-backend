package de.untenrechts.urhome;

import de.untenrechts.urhome.database.DatabaseQueries;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;


@Slf4j
public class Configuration {

    public static String readOrRuntimeException(final String pathString) {
        InputStream in = DatabaseQueries.class.getClassLoader().getResourceAsStream(pathString);
        if (in != null) {
            try {
                return new String(in.readAllBytes());
            } catch (IOException e) {
                log.error("Unable to read file {}.", pathString, e);
                throw new IllegalStateException(e);
            }
        } else {
            final String message = String.format("Unable to find file %s.", pathString);
            log.error(message);
            throw new IllegalStateException(message);
        }
    }
}
