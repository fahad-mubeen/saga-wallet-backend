package com.example.sagawallet;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SagawalletApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.load();
		dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
		System.getenv().forEach((key, value) -> {
			if (System.getProperty(key) == null) {
				System.setProperty(key, value);
			}
		});
		SpringApplication.run(SagawalletApplication.class, args);
	}

}
