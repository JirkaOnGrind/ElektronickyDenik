package com.example.authdemo.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
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

    // --- NEW UPDATE START ---

    // This creates a connection table 'vehicle_hidden_users'
    // It stores: vehicle_id | user_id
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "vehicle_hidden_users",
            joinColumns = @JoinColumn(name = "vehicle_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @ToString.Exclude // Prevents infinite recursion/StackOverflow
    @EqualsAndHashCode.Exclude // Prevents performance issues in Sets
    private Set<User> excludedUsers = new HashSet<>();

    // Helper method to hide vehicle for a user
    public void hideForUser(User user) {
        this.excludedUsers.add(user);
    }

    // Helper method to show vehicle to a user (remove ban)
    public void showToUser(User user) {
        this.excludedUsers.remove(user);
    }

    // --- NEW UPDATE END ---

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