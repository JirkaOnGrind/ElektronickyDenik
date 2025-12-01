package com.example.authdemo.service;

import com.example.authdemo.model.Company;
import com.example.authdemo.model.User;
import com.example.authdemo.repository.CompanyRepository;
import com.example.authdemo.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {
    @Autowired
    CompanyService companyService;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;


    @Autowired
    private PasswordEncoder passwordEncoder;

    // 🔐 POVINNÁ METODA PRO SPRING SECURITY
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("🔐 Spring Security hledá uživatele: " + username);

        Optional<User> dbUser = userRepository.findByEmail(username);
        if (dbUser.isEmpty()) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        User user = dbUser.get();

        if (!user.isVerificated()) {
            System.out.println("Pokus o přihlášení neověřeného uživatele: " + username);
            throw new DisabledException("User is not verified: " + username);
        }

        // Vytvoří UserDetails objekt, který Spring Security rozumí
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword()) // Musí být již zahešované!
                .roles(user.getRole()) // Základní role
                .build();
    }

    // Registrace uživatele
    public String registerUser(User user) {
        System.out.println("Register pro uživatele: " + user.getEmail());

        // Kontrola emailu
        Optional<User> existingUserByEmail = userRepository.findByEmail(user.getEmail());
        if (existingUserByEmail.isPresent()) {
            return "email_exists";
        }

        // Kontrola telefonního čísla
        Optional<User> existingUserByPhone = userRepository.findByPhone(user.getPhone());
        if (existingUserByPhone.isPresent()) {
            return "phone_exists";
        }

        // Kontrola klíče
        if (!user.getRole().equals("ADMIN") && !companyRepository.existsByKey(user.getKey())) {
            return "invalid_key";
        }

        // Hashování hesla před uložením
        String hashedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(hashedPassword);

        userRepository.save(user);
        //System.out.println("Uživatel úspěšně zaregistrován: " + user.getEmail());
        return "success";
    }
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByPhone(String phone) {
        return userRepository.findByPhone(phone);
    }

    // V UserService
    @Transactional
    public void deleteUserAndRelatedData(Long userId) {
        try {
            // 1. Nejprve smaž company (pokud existuje)
            companyService.deleteCompanyByUserId(userId);

            // 2. Pak smaž usera
            userRepository.deleteById(userId);

            System.out.println("User a related data smazány pro userId: " + userId);
        } catch (Exception e) {
            System.err.println("Chyba při mazání usera a related data: " + e.getMessage());
        }
    }

    public Optional<User> changePassword(String email, String newPassword) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return Optional.of(user);
        }
        return Optional.empty();
    }
    public void softDelete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + id));

        // THIS IS THE SOFT DELETE LOGIC:
        user.setDeletedAt(LocalDateTime.now());

        // Save the update
        userRepository.save(user);
    }

    public void changeRole(Long id, String admin) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + id));

        // THIS IS THE SOFT DELETE LOGIC:
        user.setRole(admin);

        // Save the update
        userRepository.save(user);
    }
}
