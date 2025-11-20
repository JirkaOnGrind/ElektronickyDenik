package com.example.authdemo.repository;

import com.example.authdemo.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Optional<Vehicle> findBySerialNumber(String serialNumber);
    List<Vehicle> findByBrand(String brand);
    List<Vehicle> findByType(String type);
    boolean existsBySerialNumber(String serialNumber);
    List<Vehicle> findByCompanyKey(String companyKey);
}