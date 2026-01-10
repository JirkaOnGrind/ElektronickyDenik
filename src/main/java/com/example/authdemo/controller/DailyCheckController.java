package com.example.authdemo.controller;

import com.example.authdemo.dto.DailyCheckForm;
import com.example.authdemo.model.DailyCheck;
import com.example.authdemo.model.User;
import com.example.authdemo.model.Vehicle;
import com.example.authdemo.service.DailyCheckService;
import com.example.authdemo.service.UserService;
import com.example.authdemo.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/daily-check")
public class DailyCheckController {

    @Autowired
    private DailyCheckService dailyCheckService;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private UserService userService;


    @GetMapping
    public String showDailyCheckForm(@RequestParam(value = "vehicleId") Long vehicleId,
                                     @RequestParam(value = "startDate", required = false) LocalDate startDate,
                                     @RequestParam(value = "endDate", required = false) LocalDate endDate,
                                     @RequestParam(value = "mode", required = false) String mode, // NEW PARAMETER
                                     Principal principal,
                                     Model model) {

        // 1. Validate Vehicle and User
        Optional<Vehicle> vehicleOpt = vehicleService.getVehicleById(vehicleId);
        if (vehicleOpt.isEmpty()) return "redirect:/vehicles/list";

        Optional<User> userOpt = userService.findByEmail(principal.getName());
        if (userOpt.isEmpty()) return "redirect:/login";
        User user = userOpt.get();

        Vehicle vehicle = vehicleOpt.get();

        // 2. PERMISSIONS CHECK
        boolean isGlobalAdminOrOwner = "ADMIN".equals(user.getRole()) || "OWNER".equals(user.getRole());
        // Check if user is in the vehicle's specific admin list
        boolean isVehicleAdmin = vehicle.getVehicleAdmins().stream()
                .anyMatch(admin -> admin.getId().equals(user.getId()));

        boolean canViewHistory = isGlobalAdminOrOwner || isVehicleAdmin;
        model.addAttribute("canViewHistory", canViewHistory); // Pass to view for buttons

        // 3. ROUTING LOGIC
        // If authorized to view history, AND mode is NOT explicit 'create' -> Show History (Admin View)
        if (canViewHistory && !"create".equals(mode)) {
            if (startDate == null) startDate = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
            if (endDate == null) endDate = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

            List<DailyCheck> checks = dailyCheckService.findChecksByVehicleAndDateRange(vehicle, startDate, endDate);

            model.addAttribute("vehicle", vehicle);
            model.addAttribute("checks", checks);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            model.addAttribute("user", user); // Needed for header logic

            String vehicleDisplayName = vehicle.getBrand() + " " + vehicle.getType();
            model.addAttribute("pageTitle", "Historie kontrol - " + vehicleDisplayName);

            return "daily-check-form-admin";
        }

        // 4. USER LOGIC: Show Creation Form
        DailyCheckForm dailyCheckForm = new DailyCheckForm();
        dailyCheckForm.setCheckDate(LocalDate.now());
        model.addAttribute("dailyCheckForm", dailyCheckForm);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("user", user);
        model.addAttribute("pageTitle", "Nová kontrola");

        return "daily-check-form";
    }

    @PostMapping
    public String processDailyCheck(@ModelAttribute DailyCheckForm form,
                                    Principal principal) {

        // ... (Logika zůstává stejná) ...
        Optional<User> user = userService.findByEmail(principal.getName());
        if (user.isEmpty()) return "redirect:/login";

        Optional<Vehicle> vehicle = vehicleService.getVehicleById(form.getVehicleId());
        if (vehicle.isEmpty()) return "redirect:/vehicles/list?error=vehicle_not_found";

        DailyCheck dailyCheck = new DailyCheck();
        dailyCheck.setCheckDate(form.getCheckDate() != null ? form.getCheckDate() : LocalDate.now());
        dailyCheck.setOverallResult(form.getOverallResult());
        dailyCheck.setDefectsDescription(form.getDefectsDescription());
        dailyCheck.setUser(user.get());
        dailyCheck.setVehicle(vehicle.get());

        dailyCheckService.saveDailyCheck(dailyCheck);

        return "redirect:/daily-check/success?checkId=" + dailyCheck.getId();
    }

    @GetMapping("/success")
    public String showSuccessPage(@RequestParam Long checkId,
                                  Model model,
                                  Principal principal) {

        // 1. Načtení přihlášeného uživatele (pro hlavičku)
        if (principal != null) {
            Optional<User> userOpt = userService.findByEmail(principal.getName());
            userOpt.ifPresent(user -> model.addAttribute("user", user));
        }

        Optional<DailyCheck> dailyCheck = dailyCheckService.getDailyCheckById(checkId);
        if (dailyCheck.isPresent()) {
            DailyCheck check = dailyCheck.get();
            model.addAttribute("dailyCheck", check);

            // --- PŘIDÁNO: ID vozidla pro tlačítko "Domů" ---
            model.addAttribute("vehicleId", check.getVehicle().getId());
            // -----------------------------------------------

            model.addAttribute("pageTitle", "Kontrola uložena");

            // TOTO ZŮSTÁVÁ (pro správné zobrazení v šabloně)
            model.addAttribute("STAV", DailyCheck.Stav.class);
        }

        return "daily-check-success";
    }
}