package com.example.featurewishlist.config;

import com.example.featurewishlist.model.FeatureRequest;
import com.example.featurewishlist.model.FeatureStatus;
import com.example.featurewishlist.repository.FeatureRequestRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class TestDataLoader {

    private final FeatureRequestRepository featureRequestRepository;

    @Value("${app.testdata.enabled:false}")
    private boolean testdataEnabled;

    @PostConstruct
    public void loadTestData() {
        if (!testdataEnabled) {
            System.out.println("✅ Testdaten sind deaktiviert.");
            return;
        }

        if (featureRequestRepository.count() > 0) {
            System.out.println("ℹ️ Testdaten werden nicht geladen, da bereits Daten vorhanden sind.");
            return;
        }

        System.out.println("🚀 Lade Testdaten...");

        List<FeatureRequest> features = List.of(
                FeatureRequest.builder()
                        .title("Dark Mode")
                        .description("Ein Dark Mode für die gesamte Anwendung, um die Augen zu schonen.")
                        .category("UI/UX")
                        .status(FeatureStatus.OPEN)
                        .createdAt(LocalDateTime.now().minusDays(2))
                        .build(),
                FeatureRequest.builder()
                        .title("Export als PDF")
                        .description("Export von Featurelisten als PDF-Dokument.")
                        .category("Funktion")
                        .status(FeatureStatus.IN_PROGRESS)
                        .createdAt(LocalDateTime.now().minusDays(5))
                        .build(),
                FeatureRequest.builder()
                        .title("Jira Integration")
                        .description("Automatische Verknüpfung von Features mit Jira-Tickets.")
                        .category("Integration")
                        .status(FeatureStatus.OPEN)
                        .createdAt(LocalDateTime.now().minusDays(1))
                        .ticketUrl("https://jira.example.com/browse/PROJ-123")
                        .build()
        );

        featureRequestRepository.saveAll(features);

        System.out.println("✅ Testdaten geladen.");
    }
}
