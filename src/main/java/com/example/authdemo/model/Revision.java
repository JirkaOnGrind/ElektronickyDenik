package com.example.authdemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "revisions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Revision {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "revision_date", nullable = false)
    private LocalDate revisionDate;

    // --- NOVÉ: Četnost kontroly (Enum) ---
    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false)
    private RevisionFrequency frequency;

    // --- NOVÉ: Provedl (ruční doplnění jména technika) ---
    @Column(name = "performed_by", nullable = false)
    private String performedBy;

    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    // Toto je "Zaznamenal" (automaticky přihlášený uživatel)
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false)
    private RevisionResult result;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // Závady

    // ENUMY
    public enum RevisionResult {
        BEZ_ZAVAD("Bez závad"),
        ZAVAD("Závada");

        private final String label;
        RevisionResult(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public enum RevisionFrequency {
        MESICNE_1X("1x měsíčně"),
        MESICE_3X("1x 3 měsíce");

        private final String displayName;
        RevisionFrequency(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }
}