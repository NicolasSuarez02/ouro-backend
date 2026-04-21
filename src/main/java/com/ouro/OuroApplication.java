package com.ouro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OuroApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(OuroApplication.class, args);
    }
}
