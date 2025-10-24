package com.example.authdemo.service;

import com.example.authdemo.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserCleanupService {
    private final UserRepository userRepository;

    public UserCleanupService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Spustí se každou hodinu (60 * 60 * 1000 ms)
    @Scheduled(fixedDelay = 900000)
    public void deleteUnverifiedUsers() {
        System.out.println("SPUSTENO----------------------------------------");
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(15);
        userRepository.deleteUnverifiedUsersOlderThan(threshold);
    }
}
