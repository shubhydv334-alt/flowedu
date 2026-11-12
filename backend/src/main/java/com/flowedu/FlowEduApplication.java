package com.flowedu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableJpaRepositories(considerNestedRepositories = true)
public class FlowEduApplication {
    public static void main(String[] args) {
        SpringApplication.run(FlowEduApplication.class, args);
    }
}
