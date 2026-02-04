package com.example.authdemo.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "vehicles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private VehicleCategory category;

    @Column(name = "brand", nullable = false)
    private String brand;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "registration_number")
    private String registrationNumber;

    @Column(name = "serial_number", nullable = false, unique = true)
    private String serialNumber;

    @Column(name = "capacity")
    private Double capacity;

    @Column(name = "company_key")
    private String companyKey;

    // --- 1. WHITELIST (Povolené zobrazení) ---
    // Stores ONLY users who ARE allowed to see the vehicle
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "vehicle_allowed_users",
            joinColumns = @JoinColumn(name = "vehicle_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<User> allowedUsers = new HashSet<>();

    // --- 2. VEHICLE ADMINS (Správci/Vývojáři) ---
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "vehicle_admins",
            joinColumns = @JoinColumn(name = "vehicle_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<User> vehicleAdmins = new HashSet<>();

    // --- 3. CASCADE DELETE (Smazání kontrol při smazání vozidla) ---
    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<DailyCheck> dailyChecks = new ArrayList<>();

    // --- HELPER METHODS ---

    // Visibility Logic
    public void allowUser(User user) {
        this.allowedUsers.add(user);
    }

    public void removeUserAccess(User user) {
        this.allowedUsers.remove(user);
        // If they can't see it, they can't be admin either
        this.vehicleAdmins.remove(user);
    }

    // Vehicle Admin Logic
    public void addVehicleAdmin(User user) {
        this.vehicleAdmins.add(user);
        // A vehicle admin MUST be allowed to see the vehicle
        this.allowedUsers.add(user);
    }

    public void removeVehicleAdmin(User user) {
        this.vehicleAdmins.remove(user);
    }


    // --- ENUMS ---
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
    }
}