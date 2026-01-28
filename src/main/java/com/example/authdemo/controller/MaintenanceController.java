package com.example.authdemo.controller;

import com.example.authdemo.dto.MaintenanceForm;
import com.example.authdemo.model.MaintenanceRecord;
import com.example.authdemo.model.User;
import com.example.authdemo.model.Vehicle;
import com.example.authdemo.service.CompanyService;
import com.example.authdemo.service.MaintenanceService;
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
@RequestMapping("/maintenance")
public class MaintenanceController {
    @Autowired
    private CompanyService companyService;
    @Autowired
    private MaintenanceService maintenanceService;
    @Autowired
    private VehicleService vehicleService;
    @Autowired
    private UserService userService;

    @GetMapping
    public String showMaintenanceForm(@RequestParam(value = "vehicleId") Long vehicleId,
                                      @RequestParam(value = "startDate", required = false) LocalDate startDate,
                                      @RequestParam(value = "endDate", required = false) LocalDate endDate,
                                      @RequestParam(value = "mode", required = false) String mode,
                                      Principal principal,
                                      Model model) {

        // 1. Validace
        Optional<Vehicle> vehicleOpt = vehicleService.getVehicleById(vehicleId);
        if (vehicleOpt.isEmpty()) return "redirect:/vehicles/list";

        Optional<User> userOpt = userService.findByEmail(principal.getName());
        if (userOpt.isEmpty()) return "redirect:/login";
        User user = userOpt.get();
        Vehicle vehicle = vehicleOpt.get();
        companyService.findByKey(user.getKey())
                .ifPresent(company -> model.addAttribute("companyName", company.getCompanyName()));
        // 2. Kontrola oprávnění (Admin/Owner/VehicleAdmin vidí historii)
        boolean isGlobalAdminOrOwner = "ADMIN".equals(user.getRole()) || "OWNER".equals(user.getRole()) || "SUPER_ADMIN".equals(user.getRole());
        boolean isVehicleAdmin = vehicle.getVehicleAdmins().stream()
                .anyMatch(admin -> admin.getId().equals(user.getId()));

        boolean canViewHistory = isGlobalAdminOrOwner || isVehicleAdmin;
        model.addAttribute("canViewHistory", canViewHistory);

        // 3. Rozcestník: Historie vs. Nový záznam
        if (canViewHistory && !"create".equals(mode)) {
            // Zobrazit HISTORII (Admin View)
            if (startDate == null) startDate = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
            if (endDate == null) endDate = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

            List<MaintenanceRecord> records = maintenanceService.findRecordsByVehicleAndDateRange(vehicle, startDate, endDate);

            model.addAttribute("vehicle", vehicle);
            model.addAttribute("records", records);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            model.addAttribute("user", user);
            model.addAttribute("pageTitle", "Historie údržby - " + vehicle.getBrand() + " " + vehicle.getType());

            return "maintenance-history";
        }

        // 4. Zobrazit FORMULÁŘ (User View / Admin Create Mode)
        MaintenanceForm form = new MaintenanceForm();
        form.setMaintenanceDate(LocalDate.now());

        model.addAttribute("maintenanceForm", form);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("user", user);
        model.addAttribute("pageTitle", "Nový záznam");

        return "maintenance-form";
    }

    @PostMapping
    public String processMaintenance(@ModelAttribute MaintenanceForm form, Principal principal) {
        Optional<User> user = userService.findByEmail(principal.getName());
        if (user.isEmpty()) return "redirect:/login";

        Optional<Vehicle> vehicle = vehicleService.getVehicleById(form.getVehicleId());
        if (vehicle.isEmpty()) return "redirect:/vehicles/list?error=vehicle_not_found";

        MaintenanceRecord record = new MaintenanceRecord();
        record.setMaintenanceDate(form.getMaintenanceDate() != null ? form.getMaintenanceDate() : LocalDate.now());
        record.setNextRevisionDate(form.getNextRevisionDate()); // Uložení nové revize
        record.setResult(form.getResult());
        record.setDescription(form.getDescription());
        record.setUser(user.get());
        record.setVehicle(vehicle.get());

        maintenanceService.save(record);

        return "redirect:/maintenance/success?recordId=" + record.getId();
    }

    @GetMapping("/success")
    public String showSuccessPage(@RequestParam Long recordId, Model model, Principal principal) {
        if (principal != null) {
            userService.findByEmail(principal.getName()).ifPresent(u -> model.addAttribute("user", u));
            Optional<User> userOpt = userService.findByEmail(principal.getName());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                model.addAttribute("user", user);

                // --- TOTO TI CHYBĚLO PRO HEADER ---
                companyService.findByKey(user.getKey())
                        .ifPresent(company -> model.addAttribute("companyName", company.getCompanyName()));
                // ----------------------------------
            }
        }

        Optional<MaintenanceRecord> recordOpt = maintenanceService.findById(recordId);
        if (recordOpt.isPresent()) {
            MaintenanceRecord r = recordOpt.get();

            // 1. Pro HTML část <div th:if="${record}"> ...
            model.addAttribute("record", r);

            // 2. Pro tlačítka, která používají ${maintenance.vehicle.id}
            model.addAttribute("maintenance", r);

            // 3. Pro tlačítko Domů ${vehicle.id} -> Pošleme CELÝ objekt Vehicle
            model.addAttribute("vehicle", r.getVehicle());

            model.addAttribute("pageTitle", "Údržba uložena");
        } else {
            return "redirect:/error"; // Nebo kamkoliv jinam
        }

        return "maintenance-success";
    }
}