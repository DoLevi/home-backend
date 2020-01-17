package de.untenrechts.homebackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "de.untenrechts.homebackend")
public class HomeBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(HomeBackendApplication.class, args);
	}

}
