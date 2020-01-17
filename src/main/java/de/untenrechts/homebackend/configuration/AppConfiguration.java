package de.untenrechts.homebackend.configuration;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@Getter
public class AppConfiguration {

    private final String databaseUrl;
    private final String[] corsAllowedOrigins;
    private final String corsAllowedMapping;
    private final String databaseUser;
    private final String databasePassword;

    public AppConfiguration(
            @Value("${database.url}") String databaseUrl,
            @Value("${database.user}") String databaseUser,
            @Value("${database.password}") String databasePassword,
            @Value("${cors.allowed.origins}") String corsAllowedOrigins,
            @Value("${cors.allowed.mapping}") String corsAllowedMapping) {
        this.databaseUrl = databaseUrl;
        this.corsAllowedOrigins = corsAllowedOrigins.split(",");
        this.corsAllowedMapping = corsAllowedMapping;
        this.databaseUser = databaseUser;
        this.databasePassword = databasePassword;
    }

}
