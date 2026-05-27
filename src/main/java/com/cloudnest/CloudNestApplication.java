package com.cloudnest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * CloudNestApplication — The main entry point of the application.
 *
 * @SpringBootApplication is a convenience annotation that combines:
 *   - @Configuration:       Marks this as a source of bean definitions
 *   - @EnableAutoConfiguration: Tells Spring Boot to auto-configure based on dependencies
 *   - @ComponentScan:       Scans for Spring components in this package and sub-packages
 *
 * When you run this class, Spring Boot starts an embedded Tomcat server
 * and initializes all controllers, services, and repositories.
 */
@SpringBootApplication
@EnableScheduling
public class CloudNestApplication {

    public static void main(String[] args) {
        // This single line bootstraps the entire Spring Boot application
        SpringApplication.run(CloudNestApplication.class, args);
    }
}
