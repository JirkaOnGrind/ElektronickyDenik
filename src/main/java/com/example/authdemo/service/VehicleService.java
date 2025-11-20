package com.example.authdemo.service;

import com.example.authdemo.model.User;
import com.example.authdemo.model.Vehicle;
import com.example.authdemo.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VehicleService {
    @Autowired
    private final UserService userService;
    @Autowired
    private final VehicleRepository vehicleRepository;

    public boolean registerVehicle(Vehicle vehicle) {
        System.out.println("Registrace vozíku: " + vehicle.getSerialNumber());

        // Kontrola duplicitního výrobního čísla
        if (vehicleRepository.existsBySerialNumber(vehicle.getSerialNumber())) {
            System.out.println("Výrobní číslo již existuje: " + vehicle.getSerialNumber());
            return false;
        }

        vehicleRepository.save(vehicle);
        System.out.println("Vozík úspěšně zaregistrován: " + vehicle.getSerialNumber());
        return true;
    }


    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    public List<Vehicle> getVehiclesForCurrentUser(Principal principal) {

        // 1. Zjisti, kdo je přihlášený (stejně jako minule)
        String userEmail = principal.getName();
        User currentUser = userService.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Uživatel s emailem " + userEmail + " nenalezen."));

        // 2. Získej jeho companyKey
        String companyKey = currentUser.getKey();

        // 3. Zavolej novou metodu z repository
        return vehicleRepository.findByCompanyKey(companyKey);
    }

    public List<Vehicle> getVehiclesByBrand(String brand) {
        return vehicleRepository.findByBrand(brand);
    }

    public List<Vehicle> getVehiclesByType(String type) {
        return vehicleRepository.findByType(type);
    }

    public Optional<Vehicle> getVehicleById(Long id) {
        return vehicleRepository.findById(id);
    }

    public Optional<Vehicle> getVehicleBySerialNumber(String serialNumber) {
        return vehicleRepository.findBySerialNumber(serialNumber);
    }
}