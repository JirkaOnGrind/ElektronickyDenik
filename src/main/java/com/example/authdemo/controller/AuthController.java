package com.example.authdemo.controller;

import com.example.authdemo.model.Company;
import com.example.authdemo.model.User;
import com.example.authdemo.model.Vehicle;
import com.example.authdemo.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.Principal;
import java.util.Optional;

@Controller
public class AuthController {
    @Autowired
    private EmailService emailService;
    @Autowired
    private UserService userService;
    @Autowired
    private VehicleService vehicleService;
    @Autowired
    private CompanyService companyService;
    @Autowired
    private DailyCheckService dailyCheckService;

    // INDEX NA LOGIN ------------------------------
    // --- INDEX - přesměrování na login ---
    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    // USER ----------------------------------------
    @GetMapping("/login")
    public String loginForm(Model model, HttpSession session,
                            @RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout) {
        model.addAttribute("pageTitle", "Login");

        if (error != null) {
            model.addAttribute("error", "Neplatné přihlašovací údaje!");
        }

        if (logout != null) {
            model.addAttribute("message", "Byli jste úspěšně odhlášeni.");
        }

        return "login";
    }

    // --- REGISTER GET ---------------------------------------------------------
    @GetMapping("/register")
    public String registerUserForm(Model model, HttpSession session) {
        model.addAttribute("pageTitle", "Register");
        return "registerUser";
    }

    // --- REGISTER POST -----------------------------------------------------
    @PostMapping("/auth/registerUser")
    public String registerUser(@RequestParam String firstName,
                           @RequestParam String lastName,
                           @RequestParam String email,
                           @RequestParam String phone,
                           @RequestParam String key,
                           @RequestParam String password,
                           @RequestParam String confirmPassword,
                           @RequestParam boolean terms,
                           @RequestParam boolean gdpr,
                           Model model,
                           HttpSession session)
    {

        // Validace hesla
        if (!password.equals(confirmPassword)) {
            model.addAttribute("pageTitle", "Register");
            model.addAttribute("error", "Hesla se neshodují");
            return "registerUser";
        }

        // Validace souhlasů
        if (!terms || !gdpr) {
            model.addAttribute("pageTitle", "Register");
            model.addAttribute("error", "Je nutné souhlasit s obchodními podmínkami a GDPR");
            return "registerUser";
        }

        // Vytvoření uživatele
        User user = new User(firstName, lastName, email, phone, password, key);
        String result = userService.registerUser(user);
        if ("success".equals(result)) {
            emailService.sendVerificationEmail(user);
            // Uložení do session
            session.setAttribute("pendingVerificationUserId", user.getId());
            session.setAttribute("verificationAttempts", 0);
            return "redirect:/verification";
        } else {
            model.addAttribute("pageTitle", "Register");

            // Rozlišení typu chyby podle vráceného stringu
            if ("email_exists".equals(result)) {
                model.addAttribute("error", "Email již existuje");
            } else if ("phone_exists".equals(result)) {
                model.addAttribute("error", "Telefonní číslo již existuje");
            } else if ("invalid_key".equals(result)) {
                model.addAttribute("error", "Neplatný klíč");
            } else {
                model.addAttribute("error", "Došlo k chybě při registraci");
            }

            return "registerUser";
        }
    }

    // REGISTER USER ----------------------------------------------------------
    @GetMapping("/register/company")
    public String registerCompanyForm(Model model, HttpSession session) {
        model.addAttribute("pageTitle", "Register");
        return "registerCompany";
    }

    // --- REGISTER COMPANY POST -----------------------------------------------------
    @PostMapping("/auth/registerCompany")
    public String registerCompany(@RequestParam String companyName,
                                  @RequestParam String ico,
                                  @RequestParam String address,
                                  @RequestParam(required = false) String dic,
                                  @RequestParam String key,
                                  @RequestParam String firstName,
                                  @RequestParam String lastName,
                                  @RequestParam String email,
                                  @RequestParam String phone,
                                  @RequestParam String password,
                                  @RequestParam String confirmPassword,
                                  @RequestParam boolean terms,
                                  @RequestParam boolean gdpr,
                                  Model model,
                                  HttpSession session)
    {

        // USER
        // Validace hesla
        if (!password.equals(confirmPassword)) {
            model.addAttribute("pageTitle", "Register");
            model.addAttribute("error", "Hesla se neshodují");
            return "registerUser";
        }

        // Validace souhlasů
        if (!terms || !gdpr) {
            model.addAttribute("pageTitle", "Register");
            model.addAttribute("error", "Je nutné souhlasit s obchodními podmínkami a GDPR");
            return "registerUser";
        }

        // Vytvoření uživatele
        User user = new User(firstName, lastName, email, phone, password, key);
        user.setRole("ADMIN");

        // V controlleru
        String result = userService.registerUser(user);
        if (!result.equals("success")) {
            model.addAttribute("pageTitle", "Register");

            switch(result) {
                case "email_exists":
                    model.addAttribute("error", "Email již existuje");
                    break;
                case "phone_exists":
                    model.addAttribute("error", "Telefonní číslo již existuje");
                    break;
                case "invalid_key":
                    model.addAttribute("error", "Špatný klíč");
                    break;
            }
            return "registerCompany";
        }

        // COMPANY
        // Validace hesla
        if (!password.equals(confirmPassword)) {
            model.addAttribute("pageTitle", "Registrace společnosti");
            model.addAttribute("error", "Hesla se neshodují");
            return "registerCompany";
        }

        // Validace souhlasů
        if (!terms || !gdpr) {
            model.addAttribute("pageTitle", "Registrace společnosti");
            model.addAttribute("error", "Je nutné souhlasit s obchodními podmínkami a GDPR");
            return "registerCompany";
        }

        // Validace IČO (základní kontrola - můžete přidat komplexnější validaci)
        if (ico == null || ico.trim().isEmpty()) {
            model.addAttribute("pageTitle", "Registrace společnosti");
            model.addAttribute("error", "IČO je povinné pole");
            return "registerCompany";
        }

        // Vytvoření společnosti
        Company company = new Company(companyName, ico, address, dic, user.getId(), key);

        if (companyService.registerCompany(company)) {
            emailService.sendVerificationEmail(user);
            session.setAttribute("pendingVerificationUserId", user.getId());
            session.setAttribute("verificationAttempts", 0);
            return "redirect:/verification";
        } else {
            model.addAttribute("pageTitle", "Registrace společnosti");
            model.addAttribute("error", "IČO již existuje");
            return "registerCompany";
        }
    }
    // HOME -----------------------------------------------------------
    @GetMapping("/home")
    public String showHomePage(@RequestParam(value = "vehicleId", required = false) Long vehicleId,
                               Principal principal,
                               Model model) {
        if (principal != null) {
            String email = principal.getName();
            Optional<User> user = userService.findByEmail(email);
            user.ifPresent(u -> model.addAttribute("user", u));
        }

        // Načtení vybraného vozíku
        if (vehicleId != null) {
            Optional<Vehicle> vehicle = vehicleService.getVehicleById(vehicleId);
            vehicle.ifPresent(v -> model.addAttribute("selectedVehicle", v));
        }

        model.addAttribute("pageTitle", "Domů");
        return "homeUser";
    }


    // --- ADMIN ---
    @GetMapping("/admin/dashboard")
    public String logout(HttpSession session,Model model) {
        model.addAttribute("pageTitle", "Register");
        return "homeAdmin";
    }

    // --- LOGOUT ---
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

}