package com.platform.accident;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories(basePackages = "com.platform.accident")
public class AccidentApplication {

	public static void main(String[] args) {
		SpringApplication.run(AccidentApplication.class, args);
	}

}
