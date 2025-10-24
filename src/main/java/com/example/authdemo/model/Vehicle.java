package com.example.authdemo.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vehicles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "brand", nullable = false)
    private String brand;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "serial_number", nullable = false, unique = true)
    private String serialNumber;

    @Column(name = "capacity")
    private Double capacity; // nosnost v kg

    @Column(name = "registration_number")
    private String registrationNumber; // evidenční číslo

    public Vehicle(String brand, String type, String serialNumber, Double capacity, String registrationNumber) {
        this.brand = brand;
        this.type = type;
        this.serialNumber = serialNumber;
        this.capacity = capacity;
        this.registrationNumber = registrationNumber;
    }
}