package com.example.authdemo.controller;

import com.example.authdemo.model.User;
import com.example.authdemo.model.Vehicle;
import com.example.authdemo.service.UserService;
import com.example.authdemo.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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
                                  @RequestParam Vehicle.VehicleCategory category,
                                  @RequestParam String serialNumber,
                                  @RequestParam(required = false) Double capacity,
                                  @RequestParam(required = false) String registrationNumber,
                                  Principal principal,
                                  Model model) {

        String userEmail = principal.getName();
        User currentUser = userService.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Uživatel s emailem " + userEmail + " nenalezen."));
        String companyKey = currentUser.getKey();

        Vehicle vehicle = new Vehicle();
        vehicle.setBrand(brand);
        vehicle.setType(type);
        vehicle.setCategory(category);
        vehicle.setSerialNumber(serialNumber);
        vehicle.setCapacity(capacity);
        vehicle.setRegistrationNumber(registrationNumber);
        vehicle.setCompanyKey(companyKey);

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
        // 1. Fetch vehicles (Service handles permissions)
        List<Vehicle> vehicles = vehicleService.getVehiclesForCurrentUser(principal);

        model.addAttribute("pageTitle", "Všechny vozíky");
        model.addAttribute("vehicles", vehicles);

        // 2. Check Permissions (Admin / Owner)
        boolean isAdminOrOwner = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ADMIN") || role.equals("ROLE_ADMIN") ||
                        role.equals("OWNER") || role.equals("ROLE_OWNER"));

        if (isAdminOrOwner) {
            // Admin/Owner gets the Admin View (Edit/Delete buttons)
            // Admins generally want to see the list to manage/add vehicles, so no auto-redirect here.
            return "vehicle-list-admin";
        }

        // --- NEW FEATURE: Auto-redirect for single vehicle ---
        // If it is a regular USER and they have exactly 1 vehicle, skip the list.
        if (vehicles.size() == 1) {
            return "redirect:/home?vehicleId=" + vehicles.get(0).getId();
        }
        // -----------------------------------------------------

        // Otherwise show the list (0 vehicles or 2+ vehicles)
        return "vehicle-list";
    }

    // --- DELETE ENDPOINT ---
    @PostMapping("/vehicles/delete")
    public String deleteVehicle(@RequestParam("vehicleId") Long vehicleId,
                                Authentication authentication,
                                Principal principal,
                                Model model) {

        boolean isAdminOrOwner = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ADMIN") || role.equals("ROLE_ADMIN") ||
                        role.equals("OWNER") || role.equals("ROLE_OWNER"));

        if (!isAdminOrOwner) {
            return "redirect:/vehicles/list?error=access_denied";
        }

        String userEmail = principal.getName();
        User currentUser = userService.findByEmail(userEmail).orElseThrow();
        Optional<Vehicle> vehicleOpt = vehicleService.getVehicleById(vehicleId);

        if (vehicleOpt.isPresent()) {
            Vehicle vehicle = vehicleOpt.get();
            if (vehicle.getCompanyKey().equals(currentUser.getKey())) {
                vehicleService.deleteVehicle(vehicleId);
                return "redirect:/vehicles/list?success=deleted";
            }
        }

        return "redirect:/vehicles/list?error=not_found";
    }
}