package com.example.authdemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;


@Entity
@Table(name = "daily_checks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyCheck {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Základní informace
    @Column(name = "check_date", nullable = false)
    private LocalDate checkDate;

    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Výsledek a poznámky - ZMĚNA: Použij enum místo String
    @Enumerated(EnumType.STRING)
    @Column(name = "overall_result", nullable = false)
    private Stav overallResult; // BEZ_ZAVAD nebo ZAVAD

    @Column(name = "defects_description", columnDefinition = "TEXT")
    private String defectsDescription; // Popis závad

    public DailyCheck(Vehicle vehicle, User user) {
        this.checkDate = LocalDate.now();
        this.vehicle = vehicle;
        this.user = user;
    }

    public DailyCheck(Vehicle vehicle, User user, LocalDate date) {
        this.checkDate = date;
        this.vehicle = vehicle;
        this.user = user;
    }

    // Pomocné metody pro lepší práci s enumem
    public boolean noError() {
        return overallResult == Stav.BEZ_ZAVAD;
    }

    public boolean hasError() {
        return overallResult == Stav.ZAVAD;
    }

    public enum Stav {
        BEZ_ZAVAD,
        ZAVAD
    }
}