package com.example.authdemo.controller;

import com.example.authdemo.dto.DailyCheckForm;
import com.example.authdemo.model.DailyCheck;
import com.example.authdemo.model.User;
import com.example.authdemo.model.Vehicle;
import com.example.authdemo.service.DailyCheckService;
import com.example.authdemo.service.UserService;
import com.example.authdemo.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
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
                                     Principal principal,
                                     Model model) {

        // 1. Validate Vehicle and User
        Optional<Vehicle> vehicleOpt = vehicleService.getVehicleById(vehicleId);
        if (vehicleOpt.isEmpty()) return "redirect:/vehicles/list";

        Optional<User> userOpt = userService.findByEmail(principal.getName());
        if (userOpt.isEmpty()) return "redirect:/login";
        User user = userOpt.get();

        Vehicle vehicle = vehicleOpt.get();

        // 2. ADMIN LOGIC: Load History
        if ("ADMIN".equals(user.getRole())) {
            if (startDate == null) startDate = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
            if (endDate == null) endDate = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

            List<DailyCheck> checks = dailyCheckService.findChecksByVehicleAndDateRange(vehicle, startDate, endDate);

            model.addAttribute("vehicle", vehicle);
            model.addAttribute("checks", checks);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);

            // FIX: Use brand and type instead of getName()
            String vehicleDisplayName = vehicle.getBrand() + " " + vehicle.getType();
            model.addAttribute("pageTitle", "Historie kontrol - " + vehicleDisplayName);

            return "daily-check-form-admin";
        }

        // 3. USER LOGIC: Show Empty Form (Existing code)
        DailyCheckForm dailyCheckForm = new DailyCheckForm();
        dailyCheckForm.setCheckDate(LocalDate.now());
        model.addAttribute("dailyCheckForm", dailyCheckForm);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("user", user);
        model.addAttribute("pageTitle", "Denní kontrola vozíku");

        return "daily-check-form";
    }

    @PostMapping
    public String processDailyCheck(@ModelAttribute DailyCheckForm form,
                                    Principal principal) {

        System.out.println("=== PROCESS DAILY CHECK ===");
        System.out.println("Datum: " + form.getCheckDate());
        System.out.println("Stav: " + form.getOverallResult());
        System.out.println("Popis: " + form.getDefectsDescription());
        System.out.println("Vehicle ID: " + form.getVehicleId());

        // Načtení přihlášeného uživatele
        Optional<User> user = userService.findByEmail(principal.getName());
        if (user.isEmpty()) {
            return "redirect:/login";
        }

        // Načtení vozíku podle ID z formuláře
        Optional<Vehicle> vehicle = vehicleService.getVehicleById(form.getVehicleId());
        if (vehicle.isEmpty()) {
            return "redirect:/vehicles/list?error=vehicle_not_found";
        }

        // Vytvoření DailyCheck entity z formuláře
        DailyCheck dailyCheck = new DailyCheck();
        dailyCheck.setCheckDate(form.getCheckDate() != null ? form.getCheckDate() : LocalDate.now());
        dailyCheck.setOverallResult(form.getOverallResult());
        dailyCheck.setDefectsDescription(form.getDefectsDescription());
        dailyCheck.setUser(user.get());
        dailyCheck.setVehicle(vehicle.get());
        System.out.println("RESULT: " + dailyCheck.getOverallResult());

        dailyCheckService.saveDailyCheck(dailyCheck);

        return "redirect:/daily-check/success?checkId=" + dailyCheck.getId();
    }

    // Stránka úspěchu
    @GetMapping("/success")
    public String showSuccessPage(@RequestParam Long checkId, Model model) {
        Optional<DailyCheck> dailyCheck = dailyCheckService.getDailyCheckById(checkId);
        if (dailyCheck.isPresent()) {
            model.addAttribute("dailyCheck", dailyCheck.get());
            model.addAttribute("pageTitle", "Kontrola uložena");
            model.addAttribute("STAV", DailyCheck.Stav.class);
        }
        return "daily-check-success";
    }

    //ENUM
    public enum Stav {
        ZAVAD,
        BEZ_ZAVAD
    }
}