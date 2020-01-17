package de.untenrechts.homebackend;

import de.untenrechts.homebackend.database.repositories.PurchaseRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

@SpringBootTest
@ActiveProfiles("test")
class HomeBackendApplicationTests {

	@Test
	void contextLoads() {
	}


	@Configuration
	static class TestConfig {
		@Bean
		@Profile("test")
		DataSource getDataSource() {
			return DataSourceBuilder.create().build();
		}
	}

}
