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

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false)
    private RevisionFrequency frequency;

    // Pole 'performedBy' ODSTRANĚNO. Používáme vazbu 'user'.

    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Toto pole nyní reprezentuje toho, kdo revizi provedl i zaznamenal

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false)
    private RevisionResult result;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

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