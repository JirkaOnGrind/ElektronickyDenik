package com.example.authdemo.controller;

import com.example.authdemo.model.Company;
import com.example.authdemo.model.User;
import com.example.authdemo.model.Vehicle;
import com.example.authdemo.repository.VehicleRepository;
import com.example.authdemo.service.CompanyService;
import com.example.authdemo.service.UserService;
import com.example.authdemo.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class VehicleController {
    @Autowired
    private UserService userService;
    @Autowired
    private VehicleService vehicleService;
    @Autowired
    private CompanyService companyService; // Přidáno pro získání názvu firmy
    @Autowired
    private VehicleRepository vehicleRepository;
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

        // 2. NASTAVENÍ NÁZVU FIRMY MÍSTO "Všechny vozíky"
        String pageTitle = "Všechny vozíky"; // Default
        if (principal != null) {
            User user = userService.findByEmail(principal.getName()).orElse(null);
            if (user != null) {
                Optional<Company> company = companyService.findByKey(user.getKey());
                if (company.isPresent()) {
                    pageTitle = company.get().getCompanyName();
                }
            }
        }

        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("vehicles", vehicles);

        // 3. Check Permissions (Admin / Owner)
        // Upravit v metodě showVehiclesList v VehicleController.java
        boolean isAdminOrOwner = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN") ||
                        role.equals("ROLE_OWNER") ||
                        role.equals("ROLE_SUPER_ADMIN")); // Přidáno ROLE_SUPER_ADMIN

        if (isAdminOrOwner) {
            return "vehicle-list-admin";
        }

        // --- Auto-redirect for single vehicle ---
        if (vehicles.size() == 1) {
            return "redirect:/home?vehicleId=" + vehicles.get(0).getId();
        }

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


    // --- NOVÝ ENDPOINT PRO OFFLINE DATA ---
    @GetMapping("/api/offline-vehicles")
    @ResponseBody // Důležité: Říká Springu "nevracej HTML šablonu, ale čistý JSON data"
    public List<OfflineVehicleDto> getOfflineVehicles(Principal principal) {
        if (principal == null) {
            return List.of(); // Pokud není přihlášen, vrať prázdný seznam
        }

        User user = userService.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Uživatel nenalezen"));

        // Použijeme tu novou metodu z repozitáře.
        // Posíláme ID uživatele dvakrát, protože SQL dotaz je "user_id = X OR user_id = Y"
        List<Vehicle> vehicles = vehicleRepository.findDistinctByAllowedUsers_IdOrVehicleAdmins_Id(user.getId(), user.getId());

        // Převedeme složité Entity na jednoduché DTO objekty pro mobil

        return vehicles.stream()
                .map(v -> new OfflineVehicleDto(
                        v.getId(),
                        v.getRegistrationNumber(),        // Místo getSpz() použijeme registrationNumber
                        v.getBrand() + " " + v.getType()  // Místo getName() složíme Značku + Typ (např. "Liebherr LTM 1050")
                ))
                .collect(Collectors.toList());
    }

    // Pomocná třída jen pro přenos dat (můžeš ji dát i do samostatného souboru, ale stačí tady)
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class OfflineVehicleDto {
        private Long id;
        private String spz;
        private String name;
    }

}