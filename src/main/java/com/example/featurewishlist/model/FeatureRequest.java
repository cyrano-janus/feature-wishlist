package com.example.featurewishlist.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 5000)
    private String description;

    private String category;

    // 🔽 NEU: optionaler Ticket-Link (z. B. Jira)
    @Column(length = 1024)
    private String ticketUrl;

    private LocalDateTime createdAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private FeatureStatus status = FeatureStatus.OPEN;
}
