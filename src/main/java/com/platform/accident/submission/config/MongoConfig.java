package com.platform.accident.submission.config;

import com.mongodb.client.MongoClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class MongoConfig {

    @Bean
    @Primary // <--- Αυτό λέει στο Spring "Χρησιμοποίησε εμένα, όχι το default"
    public MongoDatabaseFactory mongoDbFactory() {
        return new SimpleMongoClientDatabaseFactory(
                MongoClients.create("mongodb://localhost:27017"), "gemini-backend"
        );
    }

}
