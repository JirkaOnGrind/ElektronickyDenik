package com.example.authdemo.controller;

import com.example.authdemo.model.Company;
import com.example.authdemo.model.User;
import com.example.authdemo.repository.CompanyRepository;
import com.example.authdemo.repository.UserRepository;
import com.example.authdemo.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional; // TENTO IMPORT CHYBĚL

@Controller
@RequestMapping("/super-admin")
public class SuperAdminController {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyService companyService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, @AuthenticationPrincipal org.springframework.security.core.userdetails.User authUser) {
        List<Company> companies = companyRepository.findAll();
        User loggedUser = userRepository.findByEmail(authUser.getUsername()).orElseThrow();

        model.addAttribute("companies", companies);
        model.addAttribute("user", loggedUser);
        model.addAttribute("pageTitle", "Super Admin - Přehled firem");

        return "superAdminDashboard";
    }

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

        loggedUser.setKey(targetCompany.getKey());
        userRepository.save(loggedUser);

        redirectAttributes.addFlashAttribute("successMessage", "Byli jste přepnuti do firmy: " + targetCompany.getCompanyName());
        return "redirect:/admin/dashboard";
    }

    // Metoda pro smazání firmy
    @GetMapping("/delete-company/{companyId}")
    public String deleteCompany(@PathVariable Long companyId, RedirectAttributes redirectAttributes) {
        try {
            Optional<Company> companyOpt = companyRepository.findById(companyId);
            if (companyOpt.isPresent()) {
                // Voláme service pro kompletní promazání (včetně uživatelů)
                companyService.deleteFullCompany(companyId);
                redirectAttributes.addFlashAttribute("successMessage", "Firma i s uživateli byla úspěšně smazána.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Firma nebyla nalezena.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Chyba při mazání: " + e.getMessage());
        }
        return "redirect:/super-admin/dashboard";
    }
}