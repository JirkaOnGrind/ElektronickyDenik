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

    // --- TOTO JE TA KLÍČOVÁ ZMĚNA ---
    @Enumerated(EnumType.STRING) // Uloží do DB text "JERABY" místo čísla 0
    @Column(name = "category", nullable = false)
    private VehicleCategory category;
    // ---------------------------------

    @Column(name = "brand", nullable = false)
    private String brand; // např. "Linde", "Caterpillar"

    @Column(name = "type", nullable = false)
    private String type; // např. "H25", "336" (model)

    @Column(name = "registration_number")
    private String registrationNumber; // evidenční číslo

    @Column(name = "serial_number", nullable = false, unique = true)
    private String serialNumber;

    @Column(name = "capacity")
    private Double capacity; // nosnost v kg (necháme, jeřáby i VZV ji mají)



    @Column(name = "company_key")
    private String companyKey; // Klíč firmy

    // Ten tvůj starý konstruktor už nepotřebujeme,
    // protože máme @AllArgsConstructor od Lomboku.
    // A i kdyby, musel by teď obsahovat 'category'.
    // Takže ho klidně smaž, ať se neplete.


    public enum VehicleCategory {
        JERABY("Jeřáby"),
        STAVEBNI_STROJE("Stavební stroje"),
        MOTOROVE_VYSOKOZDVIZNE_VOZIKY("Motorové vysokozdvižné vozíky");

        private final String displayName;

        VehicleCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
        // Pokud chceš, můžeš to zkrátit, např. jen VZV
    }
}
