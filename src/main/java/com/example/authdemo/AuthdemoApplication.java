package com.example.authdemo;

import com.example.authdemo.service.EmailService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AuthdemoApplication {
    private EmailService emailService;
    public static void main(String[] args) {
        SpringApplication.run(AuthdemoApplication.class, args);

    }

}
