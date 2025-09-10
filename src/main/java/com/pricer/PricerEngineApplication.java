package com.pricer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot application for the FX Pricer Engine.
 */
@SpringBootApplication
@EnableScheduling
public class PricerEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(PricerEngineApplication.class, args);
    }
}