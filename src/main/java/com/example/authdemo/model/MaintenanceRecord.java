package com.example.authdemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "maintenance_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "maintenance_date", nullable = false)
    private LocalDate maintenanceDate;

    // NOVÉ POLE: Následující revize
    @Column(name = "next_revision_date")
    private LocalDate nextRevisionDate;

    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false)
    private MaintenanceResult result;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // Popis údržby nebo závad

    public enum MaintenanceResult {
        BEZ_ZAVAD,
        ZAVAD
    }
}