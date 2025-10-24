package com.example.authdemo.service;

import com.example.authdemo.model.Vehicle;
import com.example.authdemo.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VehicleService {
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