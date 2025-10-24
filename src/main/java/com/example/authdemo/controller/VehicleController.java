package com.example.authdemo.controller;

import com.example.authdemo.model.Vehicle;
import com.example.authdemo.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
@Controller
public class VehicleController {
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
                                  @RequestParam String serialNumber,
                                  @RequestParam(required = false) Double capacity,
                                  @RequestParam(required = false) String registrationNumber,
                                  Model model) {

        // Vytvoření vozíku
        Vehicle vehicle = new Vehicle(brand, type, serialNumber, capacity, registrationNumber);

        if (vehicleService.registerVehicle(vehicle)) {
            return "redirect:/vehicles/list";
        } else {
            model.addAttribute("pageTitle", "Registrace vozíku");
            model.addAttribute("error", "Výrobní číslo již existuje");
            return "vehicle-register";
        }
    }

    @GetMapping("/vehicles/list")
    public String showVehiclesList(Model model) {
        List<Vehicle> vehicles = vehicleService.getAllVehicles();
        model.addAttribute("pageTitle", "Všechny vozíky");
        model.addAttribute("vehicles", vehicles);
        return "vehicle-list";
    }

}
