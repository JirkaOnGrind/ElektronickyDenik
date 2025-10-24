package com.example.authdemo.controller;

import com.example.authdemo.service.EmailService;
import com.example.authdemo.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class VerificationController {
    @Autowired
    UserService userService;
    private final EmailService emailService;
    public VerificationController(EmailService emailService) {
        this.emailService = emailService;
    }

    // REGISTER USER ----------------------------------------------------------
    @GetMapping("/verification")
    public String verificationForm(Model model, HttpSession session) {
        Long userId = (Long) session.getAttribute("pendingVerificationUserId");
        if (userId == null) {
            return "redirect:/register";
        }
        model.addAttribute("pageTitle", "Verifikace");
        return "verificationEmail";
    }
    @PostMapping("/auth/verification")
    public String verifyUser(@RequestParam String code, HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("pendingVerificationUserId");

        // BEZPEČNOST SESSION
        if (userId == null) {
            return "redirect:/register";
        }

        // NAČTENÍ POČTU POKUSŮ
        Integer attempts = (Integer) session.getAttribute("verificationAttempts");
        attempts = (attempts == null) ? 1 : attempts + 1;

        // KONTROLA PŘEKROČENÍ LIMITU
        if (attempts >= 3) {
            userService.deleteUserAndRelatedData(userId);
            session.removeAttribute("pendingVerificationUserId");
            session.removeAttribute("verificationAttempts");
            model.addAttribute("pageTitle", "Verifikace");
            model.addAttribute("error", "Překročen limit pokusů. Registrujte se znovu.");
            return "verificationEmail";
        }

        // ULOŽENÍ NOVÉHO POČTU POKUSŮ
        session.setAttribute("verificationAttempts", attempts);

        // OVĚŘENÍ KÓDU
        boolean verified = emailService.checkVerificationCode(userId, code);

        if(verified) {
            // ÚSPĚCH - vyčištění session
            session.removeAttribute("pendingVerificationUserId");
            session.removeAttribute("verificationAttempts");
            return "redirect:/login?verified=true";
        } else {
            // NEÚSPĚCH - zobrazení chyby s počtem pokusů
            model.addAttribute("pageTitle", "Verifikace");
            model.addAttribute("error", "Zadaný kód není správný. Zbývající pokusy: " + (3 - attempts));
            return "verificationEmail";
        }
    }
}
