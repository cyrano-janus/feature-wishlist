package com.example.featurewishlist.view;

import com.example.featurewishlist.model.FeatureRequest;
import com.example.featurewishlist.model.FeatureStatus;
import com.example.featurewishlist.model.Vote;
import com.example.featurewishlist.repository.FeatureRequestRepository;
import com.example.featurewishlist.repository.VoteRepository;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import jakarta.servlet.http.Cookie;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Route("")
@PageTitle("Feature-Übersicht")
@AnonymousAllowed // Seite ist ohne Login sichtbar
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

        // Auth-Leiste (Login/Logout)
        HorizontalLayout authBar = buildAuthBar();

        // Aktionen: Filter + optional Add-Button
        HorizontalLayout actions = new HorizontalLayout();
        actions.add(statusFilter);
        if (isAuthenticated()) {
            Button addFeatureButton = new Button("➕ Feature hinzufügen", e -> openAddFeatureDialog());
            actions.add(addFeatureButton);
        }

        add(authBar, actions, grid);
        updateGrid(null);
    }

    private HorizontalLayout buildAuthBar() {
        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setJustifyContentMode(JustifyContentMode.BETWEEN);
        bar.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-10pct)");
        bar.getStyle().set("padding", "0.25rem 0");

        if (!isAuthenticated()) {
            Anchor login = new Anchor("login", "Login");
            bar.add(new Span("Anonym"), login);
            return bar;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "user";
        Span who = new Span("Angemeldet als: " + username);

        Button logout = new Button("Logout", e -> {
            CsrfToken csrf = (CsrfToken) VaadinService.getCurrentRequest().getAttribute("_csrf");
            String token = csrf != null ? csrf.getToken() : "";
            UI.getCurrent().getPage().executeJs(
                "fetch('logout',{method:'POST',headers:{'X-CSRF-TOKEN': $0}}).then(()=>location.assign('/'));",
                token
            );
        });

        bar.add(who, logout);
        return bar;
    }

    private void configureFilter() {
        statusFilter.setLabel("Status filtern");
        statusFilter.setItems(FeatureStatus.values());
        statusFilter.setEmptySelectionAllowed(true);
        statusFilter.setPlaceholder("Alle");
        statusFilter.addValueChangeListener(e -> updateGrid(e.getValue()));
    }

    private void configureGrid() {
        grid.addColumn(FeatureRequest::getTitle)
            .setHeader("Titel")
            .setAutoWidth(true)
            .setSortable(true);

        // Beschreibung: gekürzt + Tooltip mit vollem Text
        grid.addColumn(fr -> truncate(fr.getDescription(), 120))
            .setHeader("Beschreibung")
            .setAutoWidth(true)
            .setFlexGrow(2)
            .setSortable(false)
            .setTooltipGenerator(fr -> nullSafe(fr.getDescription()));

        grid.addColumn(FeatureRequest::getCategory)
            .setHeader("Kategorie")
            .setSortable(true);

        // Admins: editierbarer Status; Andere: read-only Spalte
        if (isAdmin()) {
            grid.addComponentColumn(this::createStatusSelector).setHeader("Status bearbeiten");
        } else {
            grid.addColumn(FeatureRequest::getStatus)
                .setHeader("Status")
                .setSortable(true);
        }

        grid.addColumn(FeatureRequest::getCreatedAt)
            .setHeader("Erstellt am")
            .setSortable(true)
            .setAutoWidth(true)
            .setKey("createdAt");

        // Ticket-Link (optional)
        grid.addComponentColumn(fr -> createTicketAnchor(fr.getTicketUrl()))
            .setHeader("Ticket");

        // Votes
        grid.addComponentColumn(this::createVoteButton).setHeader("Votes");

        grid.setAllRowsVisible(true);
    }

    private Select<FeatureStatus> createStatusSelector(FeatureRequest feature) {
        Select<FeatureStatus> statusSelect = new Select<>();
        statusSelect.setItems(FeatureStatus.values());
        statusSelect.setValue(feature.getStatus());
        statusSelect.setWidth("180px");
        statusSelect.addValueChangeListener(event -> {
            feature.setStatus(event.getValue());
            repository.save(feature);
            Notification.show("Status aktualisiert");
            updateGrid(statusFilter.getValue());
        });
        return statusSelect;
    }

    private Button createVoteButton(FeatureRequest feature) {
        long votes = voteRepository.countByFeature(feature);
        Button voteBtn = new Button("👍 " + votes);

        if (!isAuthenticated()) {
            voteBtn.setEnabled(false);
            voteBtn.getElement().setProperty("title", "Bitte einloggen, um abzustimmen");
            return voteBtn;
        }

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

    private boolean isAuthenticated() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }

    private boolean isAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
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
        if (!isAuthenticated()) {
            Notification.show("Bitte einloggen, um ein Feature hinzuzufügen.");
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Neues Feature hinzufügen");

        TextField title = new TextField("Titel");
        TextArea description = new TextArea("Beschreibung");
        TextField category = new TextField("Kategorie");

        Button save = new Button("Speichern", event -> {
            if (title.isEmpty()) {
                Notification.show("Titel ist erforderlich");
                return;
            }
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

    // ---------- Helfer ----------

    private String truncate(String text, int max) {
        if (text == null) return "";
        String t = text.trim();
        return t.length() <= max ? t : t.substring(0, max - 1) + "…";
    }

    private String nullSafe(String text) {
        return text == null ? "" : text;
    }

    private Anchor createTicketAnchor(String url) {
        if (url == null || url.isBlank()) {
            return new Anchor("", "—");
        }
        Anchor a = new Anchor(url, "Ticket");
        a.setTarget("_blank");
        a.getElement().setAttribute("rel", "noopener noreferrer");
        return a;
    }
}
