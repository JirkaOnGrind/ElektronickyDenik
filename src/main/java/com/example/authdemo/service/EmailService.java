package com.example.authdemo.service;

import com.example.authdemo.model.User;
import com.example.authdemo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class EmailService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    // TOTO JE TA ZMĚNA: Načte email (info@provoznidenik.com) z nastavení
    @Value("${spring.mail.username}")
    private String senderEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // SENDS -----------------------------------------------------------------------
    @Async
    public void sendVerificationEmail(User user)
    {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        // Zde se použije tvůj email z nastavení místo Mailtrapu
        message.setFrom(senderEmail);
        message.setSubject("Ověřovací kód");
        message.setText("Dobrý den, zde zasíláme kód od Elektronického Deníku: " + user.getVerificationKey() + "\nTento kód je platný po dobu 15 minut. Po uplynutí této doby bude váš účet automaticky smazán.");
        // Hashování kódu před uložením
        String hashedVerificationKey = passwordEncoder.encode(user.getVerificationKey());
        user.setVerificationKey(hashedVerificationKey);
        userRepository.save(user);
        mailSender.send(message);
    }

    // Pomocná funkce pro send ale s userId
    @Async
    public void sendVerificationEmailViaId(Long userId)
    {
        Optional<User> dbUser = userRepository.findById(userId);
        if (dbUser.isPresent()) {
            User user = dbUser.get();
            // Musí se vygenerovat nový, starý už je zašifrován -------------------------
            user.setVerificationKey(User.generateVerificationCode());
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            // Zde se použije tvůj email z nastavení
            message.setFrom(senderEmail);
            message.setSubject("Ověřovací kód");
            message.setText("Dobrý den, zde zasíláme kód od Elektronického Deníku: " + user.getVerificationKey() + "\nTento kód je platný po dobu 15 minut. Po uplynutí této doby bude váš účet automaticky smazán.");
            // Hashování kódu před uložením
            String hashedVerificationKey = passwordEncoder.encode(user.getVerificationKey());
            user.setVerificationKey(hashedVerificationKey);
            userRepository.save(user);
            mailSender.send(message);
        }
    }

    // Pomocná funkce pro send ale s userEmail
    @Async
    public void sendVerificationEmailViaEmail(String email)
    {
        Optional<User> dbUser = userRepository.findByEmail(email);
        if (dbUser.isPresent()) {
            User user = dbUser.get();
            // Musí se vygenerovat nový, starý už je zašifrován -------------------------
            user.setVerificationKey(User.generateVerificationCode());
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            // Zde se použije tvůj email z nastavení
            message.setFrom(senderEmail);
            message.setSubject("Ověřovací kód");
            message.setText("Dobrý den, toto je váš kód od Elektronického Deníku pro reset hesla: " + user.getVerificationKey() + "\nPokud jste o reset hesla nežádal, prosím ignorujte tento email.");
            // Hashování kódu před uložením
            String hashedVerificationKey = passwordEncoder.encode(user.getVerificationKey());
            user.setVerificationKey(hashedVerificationKey);
            userRepository.save(user);
            mailSender.send(message);
        }
    }

    // CHECKS ---------------------------------------------
    public boolean checkVerificationCode(Long userId, String code)
    {
        System.out.println("Verification attempt");
        Optional<User> dbUser = userRepository.findById(userId);
        if (dbUser.isPresent()) {
            User user = dbUser.get();
            // Kontrola hesla pomocí passwordEncoder
            boolean verifiyCodesMatches = passwordEncoder.matches(code,user.getVerificationKey());
            if(verifiyCodesMatches)
            {
                System.out.println("Verification Succesful");
                user.setVerificationKey("null");
                user.setVerificated(true);
                userRepository.save(user);
            }
            return verifiyCodesMatches;
        }
        System.out.println("User not found: " + userId);
        return false;
    }

    // POMOCNÁ FUNKCE POMOCÍ MAILU
    public boolean checkVerificationCodeViaEmail(String email, String code)
    {
        System.out.println("VERIFICATION ATTEMPT VIA EMAIL");
        Optional<User> dbUser = userRepository.findByEmail(email);
        if (dbUser.isPresent()) {
            User user = dbUser.get();
            // Kontrola hesla pomocí passwordEncoder
            boolean verifiyCodesMatches = passwordEncoder.matches(code,user.getVerificationKey());
            if(verifiyCodesMatches)
            {
                System.out.println("Verification Succesful");
                user.setVerificationKey("null");
                user.setVerificated(true);
                userRepository.save(user);
            }
            return verifiyCodesMatches;
        }
        System.out.println("User not found: " + email);
        return false;
    }
}