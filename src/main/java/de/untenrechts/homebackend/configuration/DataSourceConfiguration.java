package de.untenrechts.homebackend.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfiguration {

    private final AppConfiguration appConfiguration;

    @Autowired
    public DataSourceConfiguration(AppConfiguration appConfiguration) {
        this.appConfiguration = appConfiguration;
    }

    @Bean
    public DataSource getDataSource() {
        return DataSourceBuilder.create()
                .driverClassName("org.postgresql.Driver")
                .url(appConfiguration.getDatabaseUrl())
                .username(appConfiguration.getDatabaseUser())
                .password(appConfiguration.getDatabasePassword())
                .build();
    }

}
