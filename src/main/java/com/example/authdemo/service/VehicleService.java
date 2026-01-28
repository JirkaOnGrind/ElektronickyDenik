package com.example.authdemo.service;

import com.example.authdemo.model.User;
import com.example.authdemo.model.Vehicle;
import com.example.authdemo.repository.UserRepository;
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
    @Autowired
    private final UserRepository userRepository;

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


    // Upravit metodu v VehicleService.java
    public List<Vehicle> getVehiclesForCurrentUser(Principal principal) {
        String email = principal.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // PŘIDÁNO: Kontrola i pro SUPER_ADMIN
        if ("ADMIN".equalsIgnoreCase(user.getRole()) ||
                "OWNER".equalsIgnoreCase(user.getRole()) ||
                "SUPER_ADMIN".equalsIgnoreCase(user.getRole())) {

            // Vidí všechna vozidla firmy, jejíž klíč má aktuálně nastaven v profilu
            return vehicleRepository.findByCompanyKey(user.getKey());
        } else {
            return vehicleRepository.findVisibleVehicles(user.getKey(), user);
        }
    }


    public Optional<Vehicle> getVehicleById(Long id) {
        return vehicleRepository.findById(id);
    }

    public Optional<Vehicle> getVehicleBySerialNumber(String serialNumber) {
        return vehicleRepository.findBySerialNumber(serialNumber);
    }


    // ADDED FOR EXCLUTION OF SPECIFIC USERS
    // Method for WORKERS to see vehicles
    public List<Vehicle> getVehiclesForUser(User user) {
        // This automatically filters out the "hidden" vehicles
        return vehicleRepository.findVisibleVehicles(user.getKey(), user);
    }

    // Method for ADMIN to see ALL vehicles
    public List<Vehicle> getAllVehiclesForAdmin(User admin) {
        return vehicleRepository.findByCompanyKey(admin.getKey());
    }

    // ADMIN ACTION: Hide vehicle from specific user
    public void hideVehicleFromUser(Long vehicleId, Long userIdToBan) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        User userToBan = userRepository.findById(userIdToBan)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // REMOVE from allowed list
        vehicle.removeUserAccess(userToBan);

        vehicleRepository.save(vehicle);
    }

    // ADMIN ACTION: Show vehicle (Grant Access)
    public void restoreAccessForUser(Long vehicleId, Long userIdToAllow) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        User userToAllow = userRepository.findById(userIdToAllow)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ADD to allowed list
        vehicle.allowUser(userToAllow);

        vehicleRepository.save(vehicle);
    }
    public void deleteVehicle(Long vehicleId) {
        // JPA will verify existence and handle Cascade delete of checks
        vehicleRepository.deleteById(vehicleId);
        System.out.println("Vozidlo s ID " + vehicleId + " bylo smazáno.");
    }
}