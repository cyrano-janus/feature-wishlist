package com.example.featurewishlist.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String voterId; // z. B. UUID für Cookie/Session

    @ManyToOne
    private FeatureRequest feature;

    private LocalDateTime votedAt = LocalDateTime.now();
}
