package com.example.authdemo.controller;

import com.example.authdemo.dto.DailyCheckForm;
import com.example.authdemo.model.DailyCheck;
import com.example.authdemo.model.User;
import com.example.authdemo.model.Vehicle;
import com.example.authdemo.service.EmailService;
import com.example.authdemo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Optional;

@Controller
public class ChangePasswordController {
    @Autowired
    UserService userService;
    @Autowired
    EmailService emailService;
    @GetMapping("/changePassword")
    public String loginForm(Model model) {
        model.addAttribute("pageTitle", "Změna hesla");
        return "changePassword";
    }
    @PostMapping("/auth/send-verification")
    public String changePasswordRequest(@RequestParam String email,Model model,HttpSession session) {
        System.out.println("EMAIL - " + email);
        // Načtení přihlášeného uživatele
        Optional<User> user = userService.findByEmail(email);
        System.out.println("USER EMPTY - " + email);
        if (user.isEmpty()) {
            model.addAttribute("error", "Tento email ještě není zaregistrován");
            return "changePassword";
        }
        else
        {
            emailService.sendVerificationEmailViaEmail(email);
            session.setAttribute("verificationType", "PASSWORD_RESET");
            session.setAttribute("email", email);
        }
        return "redirect:/verification";
    }
    @GetMapping("/newPassword")
    public String newPassword(Model model, HttpSession session) {
        Boolean allowed = (Boolean) session.getAttribute("pw_reset_allowed");
        if (allowed == null || !allowed) {
            return "redirect:/login";
        }
        model.addAttribute("pageTitle", "Změna hesla");
        return "newPassword";
    }
    @PostMapping("/auth/new-password")
    public String changePasswordRequest(
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        Boolean allowed = (Boolean) session.getAttribute("pw_reset_allowed");
        String email = (String) session.getAttribute("pw_reset_email");
        if (allowed == null || !allowed || email == null) {
            redirectAttributes.addFlashAttribute("error", "Neplatná session. Zopakuj reset.");
            return "redirect:/login";
        }

        // Kontrola, zda jsou hesla stejná
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Hesla se neshodují.");
            return "newPassword";
        }

        // Změna hesla
        userService.changePassword(email, newPassword);
        // Vyčištění session po úspěšné změně hesla
        session.invalidate();
        request.getSession(true);
        redirectAttributes.addFlashAttribute("success", "Heslo bylo úspěšně změněno!");
        return "redirect:/login";
    }
}
