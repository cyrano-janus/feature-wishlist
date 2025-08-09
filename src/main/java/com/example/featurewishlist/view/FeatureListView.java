package com.example.featurewishlist.view;

import com.example.featurewishlist.model.FeatureRequest;
import com.example.featurewishlist.model.FeatureStatus;
import com.example.featurewishlist.model.Vote;
import com.example.featurewishlist.repository.FeatureRequestRepository;
import com.example.featurewishlist.repository.VoteRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;
import jakarta.servlet.http.Cookie;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Route("")
@PageTitle("Feature-Übersicht")
public class FeatureListView extends VerticalLayout {

    private final FeatureRequestRepository repository;
    private final VoteRepository voteRepository;
    private final Grid<FeatureRequest> grid = new Grid<>(FeatureRequest.class, false);
    private final Select<FeatureStatus> statusFilter = new Select<>();

    public FeatureListView(FeatureRequestRepository repository, VoteRepository voteRepository) {
        this.repository = repository;
        this.voteRepository = voteRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        configureFilter();
        configureGrid();

        Button addFeatureButton = new Button("➕ Feature hinzufügen", e -> openAddFeatureDialog());
        HorizontalLayout actions = new HorizontalLayout(statusFilter, addFeatureButton);
        add(actions, grid);

        updateGrid(null);
    }

    private boolean isAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(granted -> granted.getAuthority().equals("ROLE_ADMIN"));
    }


    private void configureFilter() {
        statusFilter.setLabel("Status filtern");
        statusFilter.setItems(FeatureStatus.values());
        statusFilter.setEmptySelectionAllowed(true);
        statusFilter.setPlaceholder("Alle");
        statusFilter.addValueChangeListener(e -> updateGrid(e.getValue()));
    }

    private void configureGrid() {
        grid.addColumn(FeatureRequest::getTitle).setHeader("Titel").setAutoWidth(true);
        grid.addColumn(FeatureRequest::getCategory).setHeader("Kategorie");
        grid.addColumn(FeatureRequest::getStatus).setHeader("Status");
        grid.addColumn(fr -> fr.getCreatedAt().toLocalDate()).setHeader("Erstellt am");
        grid.addComponentColumn(this::createVoteButton).setHeader("Votes");
        grid.setAllRowsVisible(true);
        if (isAdmin()) {
            grid.addComponentColumn(this::createStatusSelector).setHeader("Status bearbeiten");
        } else {
            grid.addColumn(FeatureRequest::getStatus).setHeader("Status");
        }
    }

    private Select<FeatureStatus> createStatusSelector(FeatureRequest feature) {
        Select<FeatureStatus> statusSelect = new Select<>();
        statusSelect.setItems(FeatureStatus.values());
        statusSelect.setValue(feature.getStatus());
        statusSelect.setWidth("150px");
        statusSelect.addValueChangeListener(event -> {
            feature.setStatus(event.getValue());
            repository.save(feature);
            Notification.show("Status aktualisiert");
        });
        return statusSelect;
    }
    
    
    
    private Button createVoteButton(FeatureRequest feature) {
        long votes = voteRepository.countByFeature(feature);
        Button voteBtn = new Button("👍 " + votes);
        voteBtn.addClickListener(e -> {
            String voterId = getOrCreateVoterId();
            Optional<Vote> existing = voteRepository.findByFeatureIdAndVoterId(feature.getId(), voterId);
            if (existing.isPresent()) {
                Notification.show("Du hast bereits abgestimmt.");
            } else {
                Vote vote = Vote.builder()
                        .feature(feature)
                        .voterId(voterId)
                        .votedAt(LocalDateTime.now())
                        .build();
                voteRepository.save(vote);
                updateGrid(statusFilter.getValue());
            }
        });
        return voteBtn;
    }

    private String getOrCreateVoterId() {
        String cookieName = "voter-id";
        String voterId = VaadinService.getCurrentRequest().getCookies() != null
                ? java.util.Arrays.stream(VaadinService.getCurrentRequest().getCookies())
                .filter(c -> cookieName.equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null)
                : null;

        if (voterId == null) {
            voterId = UUID.randomUUID().toString();
            Cookie cookie = new Cookie(cookieName, voterId);
            cookie.setPath("/");
            cookie.setMaxAge(60 * 60 * 24 * 365); // 1 Jahr
            VaadinService.getCurrentResponse().addCookie(cookie);
        }

        return voterId;
    }

    private void updateGrid(FeatureStatus filterStatus) {
        List<FeatureRequest> all = repository.findAll();
        if (filterStatus != null) {
            all = all.stream()
                    .filter(fr -> fr.getStatus() == filterStatus)
                    .collect(Collectors.toList());
        }
        grid.setItems(all);
    }

    private void openAddFeatureDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Neues Feature hinzufügen");

        TextField title = new TextField("Titel");
        TextArea description = new TextArea("Beschreibung");
        TextField category = new TextField("Kategorie");

        Button save = new Button("Speichern", event -> {
            FeatureRequest request = FeatureRequest.builder()
                    .title(title.getValue())
                    .description(description.getValue())
                    .category(category.getValue())
                    .status(FeatureStatus.OPEN)
                    .createdAt(LocalDateTime.now())
                    .build();

            repository.save(request);
            dialog.close();
            updateGrid(statusFilter.getValue());
            Notification.show("Feature gespeichert");
        });

        Button cancel = new Button("Abbrechen", e -> dialog.close());

        FormLayout formLayout = new FormLayout(title, description, category);
        HorizontalLayout buttons = new HorizontalLayout(save, cancel);
        dialog.add(new H3("Feature erstellen"), formLayout, buttons);

        dialog.open();
    }
}