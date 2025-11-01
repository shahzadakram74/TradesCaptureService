package com.example.instructions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableKafka 
@EnableAsync 
public class InstructionsApplication {
    public static void main(String[] args) {
        SpringApplication.run(InstructionsApplication.class, args);
    }
}