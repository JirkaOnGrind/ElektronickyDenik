package com.example.authdemo.controller;

import com.example.authdemo.model.User;
import com.example.authdemo.model.Vehicle;
import com.example.authdemo.repository.UserRepository;
import com.example.authdemo.service.UserService;
import com.example.authdemo.service.VehicleService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    private VehicleService vehicleService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;

    @GetMapping("/usersList")
    public String usersList(Model model, @AuthenticationPrincipal org.springframework.security.core.userdetails.User authUser) {
        // authUser.getUsername() vrátí email přihlášeného uživatele
        User loggedUser = userRepository.findByEmail(authUser.getUsername()).orElseThrow();
        List<User> users = userRepository.findByKeyAndDeletedAtIsNull(loggedUser.getKey());
        users.remove(loggedUser);
        model.addAttribute("users", users);
        model.addAttribute("pageTitle", "Uživatelé");
        return "usersList";
    }
    @GetMapping("/users/{id}")
    public String userDetail(@PathVariable Long id, Model model, @AuthenticationPrincipal org.springframework.security.core.userdetails.User authUser) {
        User loggedUser = userRepository.findByEmail(authUser.getUsername()).orElseThrow();

        // Načteme uživatele a zkontrolujeme, že je ze stejné company
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getKey().equals(loggedUser.getKey())) {
            throw new AccessDeniedException("Nemáš oprávnění zobrazit tohoto uživatele");
        }

        model.addAttribute("user", user);
        model.addAttribute("pageTitle", "Detail uživatele");
        return "userDetail"; // tady budeš mít novou HTML stránku pro detail
    }

    @GetMapping("/machines")
    public String machinesList(Model model, Principal principal) {
        List<Vehicle> vehicles = vehicleService.getVehiclesForCurrentUser(principal);
        model.addAttribute("pageTitle", "Všechny vozíky");
        model.addAttribute("vehicles", vehicles);
        return "vehicle-list-admin";
    }
    @PostMapping("users/delete/{id}")
    public String softDeleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.softDelete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Uživatel byl úspěšně smazán.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Chyba při mazání uživatele.");
        }
        // Redirect back to the list of users
        return "redirect:/admin/usersList";
    }

    // Přidání uživatele:
    @GetMapping("/users/new")
    public String usersNew(Model model) {
        model.addAttribute("pageTitle", "Přidání uživatele");
        model.addAttribute("user", new User());
        return "addUser";
    }

    @PostMapping("/users/save")
    public String saveUser(@ModelAttribute("user") User user,
                           Principal principal,
                           Model model,
                           RedirectAttributes redirectAttributes) {

        // 1. Get the currently logged-in Admin to get their KEY
        // (Assuming you have a method findByEmail in your UserService that returns Optional<User>)
        Optional<User> adminOptional = userService.findByEmail(principal.getName());

        if (adminOptional.isEmpty()) {
            return "redirect:/login"; // Safety fallback
        }

        User admin = adminOptional.get();

        // 2. Set the Critical System Fields
        // Since Admin creates the user, we copy the Admin's company key
        user.setKey(admin.getKey());

        // Set default role
        user.setRole("USER");

        // 3. Set Metadata (Simulating your Constructor logic)
        // Because Spring uses the default constructor, we must set these manually here
        user.setGdprAccepted(true);
        user.setGdprAcceptedAt(LocalDateTime.now());
        user.setTermsAccepted(true);
        user.setTermsAcceptedAt(LocalDateTime.now());
        user.setVerificated(true); // Or true, if you want admin-created users to be auto-verified
        user.setVerificationKey(UUID.randomUUID().toString()); // Generate random code
        user.setDeletedAt(null);
        // 4. Call your Service to register
        String result = userService.registerUser(user);

        // 5. Handle the result string from your Service
        if ("success".equals(result)) {
            redirectAttributes.addFlashAttribute("successMessage", "Uživatel byl úspěšně přidán.");
            return "redirect:/admin/usersList";
        }
        else {
            // Something went wrong, return to the form with the error
            if ("email_exists".equals(result)) {
                model.addAttribute("errorMessage", "Uživatel s tímto emailem již existuje.");
            } else if ("phone_exists".equals(result)) {
                model.addAttribute("errorMessage", "Uživatel s tímto telefonním číslem již existuje.");
            } else if ("invalid_key".equals(result)) {
                model.addAttribute("errorMessage", "Neplatný firemní klíč.");
            } else {
                model.addAttribute("errorMessage", "Nastala neznámá chyba.");
            }

            // Return the form view so they can correct it (data in 'user' stays populated)
            return "addUser";
        }
    }
    @PostMapping("users/promote/{id}")
    public String promoteUserToAdmin(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.changeRole(id, "ADMIN"); // or your logic
        redirectAttributes.addFlashAttribute("successMessage", "Uživatel byl povýšen na admina.");
        return "redirect:/admin/usersList"; // or redirect back to detail
    }
}
