package com.example.authdemo.controller;

import com.example.authdemo.model.User;
import com.example.authdemo.model.Vehicle;
import com.example.authdemo.service.UserService;
import com.example.authdemo.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
public class VehicleController {
    @Autowired
    private UserService userService;
    @Autowired
    private VehicleService vehicleService;
    @GetMapping("/vehicles/register")
    public String showVehicleRegistrationForm(Model model) {
        model.addAttribute("pageTitle", "Registrace vozíku");
        return "vehicle-register";
    }

    @PostMapping("/vehicles/register")
    public String registerVehicle(@RequestParam String brand,
                                  @RequestParam String type,
                                  @RequestParam Vehicle.VehicleCategory category, // <-- 1. PŘIDALI JSME KATEGORII
                                  @RequestParam String serialNumber,
                                  @RequestParam(required = false) Double capacity,
                                  @RequestParam(required = false) String registrationNumber,
                                  Principal principal,
                                  Model model) {

        String userEmail = principal.getName(); // Který uživatel přidává vehicle
        User currentUser = userService.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Uživatel s emailem " + userEmail + " nenalezen."));
        String companyKey = currentUser.getKey();

        // --- 2. ZMĚNILI JSME VYTVÁŘENÍ VOZÍKU ---
        // Teď používáme prázdný konstruktor a settery
        Vehicle vehicle = new Vehicle();
        vehicle.setBrand(brand);
        vehicle.setType(type);
        vehicle.setCategory(category); // <-- Nastavíme novou kategorii
        vehicle.setSerialNumber(serialNumber);
        vehicle.setCapacity(capacity);
        vehicle.setRegistrationNumber(registrationNumber);
        vehicle.setCompanyKey(companyKey);
        // ----------------------------------------

        if (vehicleService.registerVehicle(vehicle)) {
            return "redirect:/vehicles/list";
        } else {
            model.addAttribute("pageTitle", "Registrace vozíku");
            model.addAttribute("error", "Výrobní číslo již existuje");
            return "vehicle-register";
        }
    }

    @GetMapping("/vehicles/list")
    public String showVehiclesList(Model model, Principal principal, Authentication authentication) {
        List<Vehicle> vehicles = vehicleService.getVehiclesForCurrentUser(principal);
        model.addAttribute("pageTitle", "Všechny vozíky");
        model.addAttribute("vehicles", vehicles);
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ADMIN") || role.equals("ROLE_ADMIN"));

        if (isAdmin) {
            return "vehicle-list-admin"; // Return admin view
        }
        // --- End of Added Logic ---

        return "vehicle-list";
    }

}
