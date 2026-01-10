package com.example.authdemo.controller;

import com.example.authdemo.model.User;
import com.example.authdemo.model.Vehicle;
import com.example.authdemo.repository.UserRepository;
import com.example.authdemo.repository.VehicleRepository;
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
import java.util.ArrayList;
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
    @Autowired
    private final VehicleRepository vehicleRepository;

    public AdminController(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @GetMapping("/usersList")
    public String usersList(Model model, @AuthenticationPrincipal org.springframework.security.core.userdetails.User authUser) {
        User loggedUser = userRepository.findByEmail(authUser.getUsername()).orElseThrow();

        // Check permission (Admin or Owner)
        if (!"ADMIN".equals(loggedUser.getRole()) && !"OWNER".equals(loggedUser.getRole())) {
            throw new AccessDeniedException("Nemáte oprávnění přistupovat k seznamu uživatelů.");
        }

        // Fetch all users in the company
        List<User> users = userRepository.findByKeyAndDeletedAtIsNull(loggedUser.getKey());

        // Remove self from the list
        users.removeIf(u -> u.getId().equals(loggedUser.getId()));

        // --- FILTERING LOGIC ---
        // If logged in as ADMIN, hide OWNER and other ADMINs
        if ("ADMIN".equals(loggedUser.getRole())) {
            users.removeIf(u -> "OWNER".equals(u.getRole()) || "ADMIN".equals(u.getRole()));
        }
        // Owner sees everyone (no extra filtering needed)

        model.addAttribute("users", users);
        model.addAttribute("loggedUser", loggedUser);
        model.addAttribute("pageTitle", "Uživatelé");
        return "usersList";
    }
    @GetMapping("/users/{id}")
    public String userDetail(@PathVariable Long id, Model model, @AuthenticationPrincipal org.springframework.security.core.userdetails.User authUser) {
        User loggedUser = userRepository.findByEmail(authUser.getUsername()).orElseThrow();

        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 1. Basic Company Check
        if (!targetUser.getKey().equals(loggedUser.getKey())) {
            throw new AccessDeniedException("Nemáš oprávnění zobrazit tohoto uživatele");
        }

        // 2. HIERARCHY CHECK (New)
        // Admin cannot view Owner or other Admins
        if ("ADMIN".equals(loggedUser.getRole())) {
            if ("OWNER".equals(targetUser.getRole()) || "ADMIN".equals(targetUser.getRole())) {
                throw new AccessDeniedException("Nemáte oprávnění spravovat administrátory nebo vlastníka.");
            }
        }

        List<Vehicle> allVehicles = vehicleRepository.findByCompanyKey(targetUser.getKey());

        model.addAttribute("user", targetUser);
        model.addAttribute("loggedUser", loggedUser);
        model.addAttribute("vehicles", allVehicles);
        model.addAttribute("pageTitle", "Detail uživatele");

        return "userDetail";
    }
    // Ukládání uživatele

    @PostMapping("/users/{userId}/permissions")
    public String updateUserPermissions(@PathVariable Long userId,
                                        @RequestParam(required = false) List<Long> allowedVehicleIds,
                                        @RequestParam(required = false) List<Long> vehicleAdminIds, // <-- NEW PARAMETER
                                        @AuthenticationPrincipal org.springframework.security.core.userdetails.User authUser) {

        User loggedUser = userRepository.findByEmail(authUser.getUsername()).orElseThrow();
        User targetUser = userRepository.findById(userId).orElseThrow();

        // Security check
        if (!targetUser.getKey().equals(loggedUser.getKey())) {
            throw new AccessDeniedException("Nemáš oprávnění.");
        }

        // Initialize lists if null
        if (allowedVehicleIds == null) allowedVehicleIds = new ArrayList<>();
        if (vehicleAdminIds == null) vehicleAdminIds = new ArrayList<>();

        List<Vehicle> companyVehicles = vehicleRepository.findByCompanyKey(targetUser.getKey());

        for (Vehicle vehicle : companyVehicles) {

            // 1. Handle "Vehicle Admin" (Developer)
            if (vehicleAdminIds.contains(vehicle.getId())) {
                vehicle.addVehicleAdmin(targetUser);

                // If user is Admin of vehicle, they MUST be able to see it
                if (!allowedVehicleIds.contains(vehicle.getId())) {
                    allowedVehicleIds.add(vehicle.getId());
                }
            } else {
                vehicle.removeVehicleAdmin(targetUser);
            }

            // 2. Handle Visibility
            if (allowedVehicleIds.contains(vehicle.getId())) {
                vehicle.allowUser(targetUser); // Use whitelist method
            } else {
                vehicle.removeUserAccess(targetUser); // Use whitelist method
            }

            vehicleRepository.save(vehicle);
        }

        return "redirect:/admin/users/" + userId + "?success";
    }
    @GetMapping("/machines")
    public String machinesList(Model model, Principal principal) {
        List<Vehicle> vehicles = vehicleService.getVehiclesForCurrentUser(principal);
        model.addAttribute("pageTitle", "Všechny vozíky");
        model.addAttribute("vehicles", vehicles);
        return "vehicle-list-admin";
    }
    @PostMapping("users/delete/{id}")
    public String softDeleteUser(@PathVariable Long id,
                                 @AuthenticationPrincipal org.springframework.security.core.userdetails.User authUser,
                                 RedirectAttributes redirectAttributes) {
        User loggedUser = userRepository.findByEmail(authUser.getUsername()).orElseThrow();
        User targetUser = userRepository.findById(id).orElseThrow();

        // OWNER can delete anyone (except themselves, technically)
        if ("OWNER".equals(loggedUser.getRole())) {
            // Allowed
        }
        // ADMIN can ONLY delete USER (not OWNER, not other ADMIN)
        else if ("ADMIN".equals(loggedUser.getRole())) {
            if ("ADMIN".equals(targetUser.getRole()) || "OWNER".equals(targetUser.getRole())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Nemůžete smazat jiného administrátora nebo vlastníka.");
                return "redirect:/admin/users/" + id;
            }
        }
        // Others cannot delete
        else {
            throw new AccessDeniedException("Nemáte oprávnění mazat uživatele.");
        }

        try {
            userService.softDelete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Uživatel byl úspěšně smazán.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Chyba při mazání uživatele.");
        }
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
    public String promoteUserToAdmin(@PathVariable Long id,
                                     @AuthenticationPrincipal org.springframework.security.core.userdetails.User authUser,
                                     RedirectAttributes redirectAttributes) {
        User loggedUser = userRepository.findByEmail(authUser.getUsername()).orElseThrow();

        // Only OWNER can promote
        if (!"OWNER".equals(loggedUser.getRole())) {
            throw new AccessDeniedException("Pouze vlastník (OWNER) může jmenovat administrátory.");
        }

        userService.changeRole(id, "ADMIN");
        redirectAttributes.addFlashAttribute("successMessage", "Uživatel byl povýšen na admina.");
        return "redirect:/admin/usersList";
    }

    // 1. Zobrazení seznamu uživatelů pro konkrétní vozidlo
    // --- NOVÁ METODA PRO SPRÁVCE (Zobrazit uživatele/adminy vozíku) ---
    // Toto řeší tvůj problém s tlačítkem "Správce"
    @GetMapping("/vehicles/{vehicleId}/users")
    public String vehicleUsersList(@PathVariable Long vehicleId,
                                   Model model,
                                   @AuthenticationPrincipal org.springframework.security.core.userdetails.User authUser) {

        // Načtení přihlášeného uživatele
        User loggedUser = userRepository.findByEmail(authUser.getUsername()).orElseThrow();

        // Načtení vozidla
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        // KONTROLA OPRÁVNĚNÍ: Je uživatel Globální Admin NEBO Správce tohoto vozíku?
        boolean isGlobalAdmin = "ADMIN".equals(loggedUser.getRole()) || "OWNER".equals(loggedUser.getRole());
        boolean isVehicleAdmin = vehicle.getVehicleAdmins().stream()
                .anyMatch(admin -> admin.getId().equals(loggedUser.getId()));

        // Pokud vozidlo nepatří pod firmu uživatele NEBO uživatel nemá práva -> Access Denied
        if (!vehicle.getCompanyKey().equals(loggedUser.getKey()) || (!isGlobalAdmin && !isVehicleAdmin)) {
            throw new AccessDeniedException("Nemáte oprávnění spravovat toto vozidlo.");
        }

        // Načtení seznamu uživatelů firmy
        List<User> companyUsers = userRepository.findByKeyAndDeletedAtIsNull(loggedUser.getKey());

        // Odstranit sebe sama ze seznamu (aby si správce nemohl omylem zrušit práva)
        companyUsers.removeIf(u -> u.getId().equals(loggedUser.getId()));

        model.addAttribute("vehicle", vehicle);
        model.addAttribute("users", companyUsers);

        // --- DŮLEŽITÉ: TENTO ŘÁDEK TAM MUSÍ BÝT, JINAK TO SPADNE ---
        model.addAttribute("user", loggedUser);
        // -----------------------------------------------------------

        return "vehicleUserList";
    }

    // 2. Uložení oprávnění (POST)
    @PostMapping("/vehicles/{vehicleId}/permissions")
    public String updateVehiclePermissions(@PathVariable Long vehicleId,
                                           @RequestParam(required = false) List<Long> allowedUserIds,
                                           @RequestParam(required = false) List<Long> vehicleAdminIds,
                                           @AuthenticationPrincipal org.springframework.security.core.userdetails.User authUser) {

        User loggedUser = userRepository.findByEmail(authUser.getUsername()).orElseThrow();
        Vehicle vehicle = vehicleRepository.findById(vehicleId).orElseThrow();

        // --- FIXED SECURITY CHECK (Compare IDs) ---
        boolean isGlobalAdmin = "ADMIN".equals(loggedUser.getRole());
        boolean isVehicleAdmin = vehicle.getVehicleAdmins().stream()
                .anyMatch(admin -> admin.getId().equals(loggedUser.getId()));

        if (!vehicle.getCompanyKey().equals(loggedUser.getKey()) || (!isGlobalAdmin && !isVehicleAdmin)) {
            throw new AccessDeniedException("Nemáte oprávnění spravovat toto vozidlo.");
        }
        // ------------------------------------------

        if (allowedUserIds == null) allowedUserIds = new ArrayList<>();
        if (vehicleAdminIds == null) vehicleAdminIds = new ArrayList<>();

        List<User> companyUsers = userRepository.findByKeyAndDeletedAtIsNull(loggedUser.getKey());

        for (User user : companyUsers) {

            // CRITICAL FIX: Skip the logged-in user
            // Since we hid them in the form, they are NOT in the submitted IDs lists.
            // If we don't skip them, the 'else' blocks below would remove their permissions.
            if (user.getId().equals(loggedUser.getId())) {
                continue;
            }

            // 1. Handle "Vehicle Admin" Role (Developer)
            if (vehicleAdminIds.contains(user.getId())) {
                vehicle.addVehicleAdmin(user);
                // Admins must be able to see the vehicle
                if (!allowedUserIds.contains(user.getId())) {
                    allowedUserIds.add(user.getId());
                }
            } else {
                vehicle.removeVehicleAdmin(user);
            }

            // 2. Handle "Allowed" Visibility
            if (allowedUserIds.contains(user.getId())) {
                vehicle.allowUser(user);
            } else {
                vehicle.removeUserAccess(user);
            }
        }

        vehicleRepository.save(vehicle);

        return "redirect:/admin/vehicles/" + vehicleId + "/users?success";
    }

    @PostMapping("users/demote/{id}")
    public String demoteAdminToUser(@PathVariable Long id,
                                    @AuthenticationPrincipal org.springframework.security.core.userdetails.User authUser,
                                    RedirectAttributes redirectAttributes) {

        User loggedUser = userRepository.findByEmail(authUser.getUsername()).orElseThrow();
        User targetUser = userRepository.findById(id).orElseThrow();

        // 1. Security Check: Only OWNER can demote
        if (!"OWNER".equals(loggedUser.getRole())) {
            throw new AccessDeniedException("Pouze vlastník (OWNER) může odebírat oprávnění.");
        }

        // 2. Validate Target
        if ("OWNER".equals(targetUser.getRole())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Nemůžete odebrat oprávnění vlastníkovi.");
            return "redirect:/admin/users/" + id;
        }

        // 3. Perform Demotion
        userService.changeRole(id, "USER");

        redirectAttributes.addFlashAttribute("successMessage", "Uživateli byla odebrána administrátorská práva.");
        return "redirect:/admin/usersList";
    }
}
