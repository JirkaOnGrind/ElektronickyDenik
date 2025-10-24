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
                                     Principal principal,
                                     Model model) {

        // Kontrola, zda vozík existuje
        Optional<Vehicle> vehicle = vehicleService.getVehicleById(vehicleId);
        if (vehicle.isEmpty()) {
            return "redirect:/vehicles/list?error=vehicle_not_found";
        }

        // Načtení přihlášeného uživatele
        Optional<User> user = userService.findByEmail(principal.getName());
        if (user.isEmpty()) {
            return "redirect:/login";
        }

        // Vytvoření DailyCheckForm (DTO)
        DailyCheckForm dailyCheckForm = new DailyCheckForm();
        dailyCheckForm.setCheckDate(LocalDate.now()); // Automaticky dnešní datum

        model.addAttribute("dailyCheckForm", dailyCheckForm);
        model.addAttribute("vehicle", vehicle.get()); // Pro zobrazení informací
        model.addAttribute("user", user.get());       // Pro zobrazení informací
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