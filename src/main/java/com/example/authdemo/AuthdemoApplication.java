package com.example.authdemo;

import com.example.authdemo.model.User;
import com.example.authdemo.repository.UserRepository;
import com.example.authdemo.service.EmailService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication; // Předpokládám, že tohle tam máš
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

@SpringBootApplication
@EnableScheduling
public class AuthdemoApplication {
    private EmailService emailService;
    public static void main(String[] args) {
        SpringApplication.run(AuthdemoApplication.class, args);

    }
    /*
    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {

        // Lambda výraz (args -> { ... }) je kód, který se spustí
        return args -> {

            // 1. Zkontrolujeme, jestli uživatel už náhodou neexistuje
            String adminEmail = "karel.novak@example.com";
            String adminEmailBurger = "jirkanetrh@gmail.com";
            if (userRepository.findByEmail(adminEmail).isEmpty()) {

                System.out.println("--- Vytvářím testovací uživatele pro 'Pizza' ---");

                // 2. Vytvoříme ADMINA
                User adminPizza = new User();
                adminPizza.setFirstName("Karel");
                adminPizza.setLastName("Novák");
                adminPizza.setEmail(adminEmail);
                adminPizza.setPassword(passwordEncoder.encode("password123")); // Hashujeme heslo
                adminPizza.setPhone("603111222");
                adminPizza.setKey("PIZZA"); // Nový klíč pro "Pizza"
                adminPizza.setRole("ADMIN");
                adminPizza.setDeletedAt(null);

                User adminBurger = new User();
                adminBurger.setFirstName("Jiří");
                adminBurger.setLastName("Netrh");
                adminBurger.setEmail(adminEmailBurger);
                adminBurger.setPassword(passwordEncoder.encode("Burger")); // Hashujeme heslo
                adminBurger.setPhone("6031112222");
                adminBurger.setKey("Burger"); // Nový klíč pro "Pizza"
                adminBurger.setRole("ADMIN");
                adminBurger.setDeletedAt(null);
                // Souhlasy
                adminPizza.setGdprAccepted(true);
                adminPizza.setGdprAcceptedAt(LocalDateTime.now());
                adminPizza.setTermsAccepted(true);
                adminPizza.setTermsAcceptedAt(LocalDateTime.now());

                adminBurger.setGdprAccepted(true);
                adminBurger.setGdprAcceptedAt(LocalDateTime.now());
                adminBurger.setTermsAccepted(true);
                adminBurger.setTermsAcceptedAt(LocalDateTime.now());

                // Verifikace
                adminPizza.setVerificated(true); // Dáme true, aby se mohl přihlásit
                adminBurger.setVerificated(true);
                // --- OPRAVA CHYBY ---
                // Místo 'null' nastavíme cokoliv, jen ne null.
                adminPizza.setVerificationKey("placeholder-key-admin");
                adminBurger.setVerificationKey("placeholder-key-admin");
                // 3. Vytvoříme USERA
                User userPizza2 = new User();
                User userPizza = new User();
                userPizza.setFirstName("Jana");
                userPizza.setLastName("Dvořáková");
                userPizza.setEmail("jana.dvorakova@example.com");
                userPizza.setPassword(passwordEncoder.encode("password123")); // Hashujeme heslo
                userPizza.setPhone("604333444");
                userPizza.setKey("PIZZA"); // Stejný klíč firmy
                userPizza.setRole("USER");
                userPizza.setDeletedAt(null);

                userPizza2.setFirstName("Jiří");
                userPizza2.setLastName("Nigr");
                userPizza2.setEmail("jirkanetrh06@gmail.com");
                userPizza2.setPassword(passwordEncoder.encode("Burger")); // Hashujeme heslo
                userPizza2.setPhone("6043334444");
                userPizza2.setKey("Burger"); // Stejný klíč firmy
                userPizza2.setRole("USER");
                userPizza2.setDeletedAt(null);
                // Souhlasy
                userPizza.setGdprAccepted(true);
                userPizza.setGdprAcceptedAt(LocalDateTime.now());
                userPizza.setTermsAccepted(true);
                userPizza.setTermsAcceptedAt(LocalDateTime.now());
                userPizza2.setGdprAccepted(true);
                userPizza2.setGdprAcceptedAt(LocalDateTime.now());
                userPizza2.setTermsAccepted(true);
                userPizza2.setTermsAcceptedAt(LocalDateTime.now());
                // Verifikace
                userPizza.setVerificated(true);
                userPizza2.setVerificated(true);
                // --- OPRAVA CHYBY ---
                userPizza.setVerificationKey("placeholder-key-user");
                userPizza2.setVerificationKey("placeholder-key-user");
                // 4. Uložíme oba do databáze
                userRepository.saveAll(List.of(adminPizza, userPizza, adminBurger,userPizza2));
                System.out.println("--- TESTOVACÍ UŽIVATELÉ 'PIZZA' BYLI VYTVOŘENI. ---");

            } else {
                System.out.println("--- Testovací uživatelé 'Pizza' již existují, přeskočeno. ---");
            }
        };
    }
    */

}
