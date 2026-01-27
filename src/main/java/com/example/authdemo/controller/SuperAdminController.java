package com.example.authdemo.controller;

import com.example.authdemo.model.Company;
import com.example.authdemo.model.User;
import com.example.authdemo.repository.CompanyRepository;
import com.example.authdemo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/super-admin")
public class SuperAdminController {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    // Zobrazení seznamu všech firem
    @GetMapping("/dashboard")
    public String dashboard(Model model, @AuthenticationPrincipal org.springframework.security.core.userdetails.User authUser) {
        // Načteme všechny firmy
        List<Company> companies = companyRepository.findAll();

        // Načteme aktuálně přihlášeného Super Admina pro zobrazení jména
        User loggedUser = userRepository.findByEmail(authUser.getUsername()).orElseThrow();

        model.addAttribute("companies", companies);
        model.addAttribute("user", loggedUser);
        model.addAttribute("pageTitle", "Super Admin - Přehled firem");

        return "superAdminDashboard";
    }

    // Akce pro přepnutí do konkrétní firmy
    @GetMapping("/switch-company/{companyId}")
    public String switchCompany(@PathVariable Long companyId,
                                @AuthenticationPrincipal org.springframework.security.core.userdetails.User authUser,
                                RedirectAttributes redirectAttributes) {

        User loggedUser = userRepository.findByEmail(authUser.getUsername()).orElseThrow();
        Company targetCompany = companyRepository.findById(companyId).orElse(null);

        if (targetCompany == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Firma nenalezena.");
            return "redirect:/super-admin/dashboard";
        }

        // --- CORE LOGIKA ---
        // Změníme klíč Super Admina na klíč vybrané firmy.
        // Tím pádem pro zbytek systému (AdminController, VehicleService) bude vypadat,
        // jako že patří do této firmy.
        loggedUser.setKey(targetCompany.getKey());
        userRepository.save(loggedUser);

        redirectAttributes.addFlashAttribute("successMessage", "Byli jste přepnuti do firmy: " + targetCompany.getCompanyName());

        // Přesměrujeme ho rovnou do Admin Dashboardu té firmy
        return "redirect:/admin/dashboard";
    }
}