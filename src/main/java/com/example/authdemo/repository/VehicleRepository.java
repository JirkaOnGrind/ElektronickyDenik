package com.example.authdemo.repository;

import com.example.authdemo.model.User;
import com.example.authdemo.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Optional<Vehicle> findBySerialNumber(String serialNumber);
    List<Vehicle> findByBrand(String brand);
    List<Vehicle> findByType(String type);
    boolean existsBySerialNumber(String serialNumber);
    List<Vehicle> findByCompanyKey(String companyKey);
    @Query("SELECT v FROM Vehicle v WHERE v.companyKey = :companyKey AND :user NOT MEMBER OF v.excludedUsers")
    List<Vehicle> findVisibleVehicles(@Param("companyKey") String companyKey, @Param("user") User user);
}