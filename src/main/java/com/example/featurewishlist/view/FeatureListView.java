package com.example.featurewishlist.view;

import com.example.featurewishlist.model.FeatureRequest;
import com.example.featurewishlist.model.FeatureStatus;
import com.example.featurewishlist.model.Vote;
import com.example.featurewishlist.repository.FeatureRequestRepository;
import com.example.featurewishlist.repository.VoteRepository;
import com.example.featurewishlist.ui.ThemeUtil;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.renderer.ComponentRenderer;
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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Route("")
@PageTitle("Feature-Übersicht")
@AnonymousAllowed
@CssImport("./styles/feature-list.css")
public class FeatureListView extends VerticalLayout {

    private final FeatureRequestRepository repository;
    private final VoteRepository voteRepository;

    private final VirtualList<FeatureRequest> list = new VirtualList<>();
    private final Select<FeatureStatus> statusFilter = new Select<>();

    public FeatureListView(FeatureRequestRepository repository, VoteRepository voteRepository) {
        this.repository = repository;
        this.voteRepository = voteRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // Dark Mode anwenden (Cookie/System)
        ThemeUtil.applySavedThemeOrSystemDefault();

        // Header (Login/Logout/Admin + Theme-Toggle)
        HorizontalLayout header = buildHeader();

        // Filter + "Feature hinzufügen" (nur eingeloggt)
        HorizontalLayout actions = buildActions();

        configureList();

        add(header, actions, list);
        updateList(null);
    }

    private HorizontalLayout buildHeader() {
        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setJustifyContentMode(JustifyContentMode.BETWEEN);
        bar.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-10pct)");
        bar.getStyle().set("padding", "0.25rem 0");

        // left: who / login
        HorizontalLayout left = new HorizontalLayout();
        left.setSpacing(true);

        if (!isAuthenticated()) {
            Anchor login = new Anchor("login", "Login");
            left.add(new Span("Anonym"), login);
        } else {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth != null ? auth.getName() : "user";
            Span who = new Span("Angemeldet als: " + username);
            left.add(who);

            if (isAdmin()) {
                Anchor admin = new Anchor("admin", "Admin");
                left.add(admin);
            }

            Button logout = new Button("Logout", e -> {
                CsrfToken csrf = (CsrfToken) VaadinService.getCurrentRequest().getAttribute("_csrf");
                String token = csrf != null ? csrf.getToken() : "";
                UI.getCurrent().getPage().executeJs(
                    "fetch('logout',{method:'POST',headers:{'X-CSRF-TOKEN': $0}}).then(()=>location.assign('/'));",
                    token
                );
            });
            left.add(logout);
        }

        // right: theme toggle
        Button themeToggle = new Button(new Icon(ThemeUtil.isDark() ? VaadinIcon.SUN_O : VaadinIcon.MOON_O));
        themeToggle.getElement().setProperty("title", "Theme umschalten (Hell/Dunkel)");
        themeToggle.addClickListener(e -> {
            ThemeUtil.toggle();
            themeToggle.setIcon(new Icon(ThemeUtil.isDark() ? VaadinIcon.SUN_O : VaadinIcon.MOON_O));
        });

        bar.add(left, themeToggle);
        bar.setAlignItems(Alignment.CENTER);
        return bar;
    }

    private HorizontalLayout buildActions() {
        statusFilter.setLabel("Status filtern");
        statusFilter.setItems(FeatureStatus.values());
        statusFilter.setEmptySelectionAllowed(true);
        statusFilter.setPlaceholder("Alle");
        statusFilter.addValueChangeListener(e -> updateList(e.getValue()));

        HorizontalLayout actions = new HorizontalLayout(statusFilter);
        if (isAuthenticated()) {
            Button addFeatureButton = new Button("➕ Feature hinzufügen", e -> openAddFeatureDialog());
            actions.add(addFeatureButton);
        }
        return actions;
    }

    private void configureList() {
        list.setRenderer(new ComponentRenderer<>(this::createFeatureCard));
        list.setHeightFull();
        addClassName("feature-list-container");
    }

    private VerticalLayout createFeatureCard(FeatureRequest fr) {
        // LEFT: große Vote-Zahl + Label
        long votes = voteRepository.countByFeature(fr);
        Span votesNum = new Span(String.valueOf(votes));
        votesNum.addClassName("votes-num");
        Span votesLbl = new Span("votes");
        votesLbl.addClassName("votes-label");

        VerticalLayout left = new VerticalLayout(votesNum, votesLbl);
        left.setPadding(false);
        left.setSpacing(false);
        left.setAlignItems(Alignment.CENTER);
        left.addClassName("votes-box");

        // RIGHT: Titel, Beschreibung, Meta-Zeile (Status-Pill, Ticket, Vote-Button)
        H4 title = new H4(fr.getTitle() != null ? fr.getTitle() : "(ohne Titel)");
        title.addClassName("feature-title");

        Paragraph desc = new Paragraph(fr.getDescription() != null ? fr.getDescription() : "");
        desc.addClassName("feature-desc");

        Span statusPill = new Span(mapStatusLabel(fr.getStatus()));
        var themes = statusPill.getElement().getThemeList();
        themes.add("badge");
        themes.add("pill");
        themes.add("small");
        statusPill.addClassNames("status-pill", pillClass(fr.getStatus()));

        Anchor ticket = createTicketAnchor(fr.getTicketUrl());
        ticket.addClassName("ticket-link");

        Button voteBtn = buildVoteButton(fr, votes);

        HorizontalLayout meta = new HorizontalLayout(statusPill, ticket, voteBtn);
        meta.setSpacing(true);
        meta.setAlignItems(Alignment.CENTER);
        meta.addClassName("meta-row");

        VerticalLayout right = new VerticalLayout(title, desc, meta);
        right.setPadding(false);
        right.setSpacing(false);
        right.addClassName("card-right");

        // CARD Container
        HorizontalLayout row = new HorizontalLayout(left, right);
        row.setWidthFull();
        row.setAlignItems(Alignment.START);
        row.setJustifyContentMode(JustifyContentMode.START);

        VerticalLayout card = new VerticalLayout(row);
        card.setPadding(true);
        card.setSpacing(false);
        card.addClassName("feature-card");
        return card;
    }

    private Button buildVoteButton(FeatureRequest feature, long currentVotes) {
        Button voteBtn = new Button(new Icon(VaadinIcon.THUMBS_UP_O));
        voteBtn.setText(String.valueOf(currentVotes));

        if (!isAuthenticated()) {
            voteBtn.setEnabled(false);
            voteBtn.getElement().setProperty("title", "Bitte einloggen, um abzustimmen");
            return voteBtn;
        }

        voteBtn.addClickListener(e -> {
            handleVote(feature);
            voteBtn.setText(String.valueOf(voteRepository.countByFeature(feature)));
            // Liste neu sortieren, damit „meist gevotet zuerst“ live bleibt
            updateList(statusFilter.getValue());
        });

        return voteBtn;
    }

    private void handleVote(FeatureRequest feature) {
        String voterId = getOrCreateVoterId();
        Optional<Vote> existing = voteRepository.findByFeatureIdAndVoterId(feature.getId(), voterId);
        if (existing.isPresent()) {
            Notification.show("Du hast bereits abgestimmt.");
            return;
        }
        Vote vote = Vote.builder()
                .feature(feature)
                .voterId(voterId)
                .votedAt(LocalDateTime.now())
                .build();
        voteRepository.save(vote);
    }

    private void updateList(FeatureStatus filterStatus) {
        List<FeatureRequest> all = repository.findAll();
        if (filterStatus != null) {
            all = all.stream()
                    .filter(fr -> fr.getStatus() == filterStatus)
                    .collect(Collectors.toList());
        }
        // meisten Votes zuerst
        all = all.stream()
                 .sorted(Comparator.comparingLong((FeatureRequest fr) -> voteRepository.countByFeature(fr)).reversed())
                 .toList();
        list.setItems(all);
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
            updateList(statusFilter.getValue());
            Notification.show("Feature gespeichert");
        });

        Button cancel = new Button("Abbrechen", e -> dialog.close());

        FormLayout formLayout = new FormLayout(title, description, category);
        HorizontalLayout buttons = new HorizontalLayout(save, cancel);
        dialog.add(new H3("Feature erstellen"), formLayout, buttons);

        dialog.open();
    }

    // ---------- Helpers ----------

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

    private String mapStatusLabel(FeatureStatus s) {
        if (s == null || s == FeatureStatus.OPEN) {
            return "Open";
        }
        if (s == FeatureStatus.IN_PROGRESS) {
            return "In Progess";
        }
        if (s == FeatureStatus.REJECTED) {
            return "Rejected";
        }
        // DONE (oder alles andere)
        return "Done";
    }

    private String pillClass(FeatureStatus s) {
        if (s == null || s == FeatureStatus.OPEN) {
            return "pill-contrast";
        }
        if (s == FeatureStatus.IN_PROGRESS) {
            return "pill-primary";
        }        
        if (s == FeatureStatus.REJECTED) {
            return "pill-error  ";
        }
        // DONE (oder alles andere)
        return "pill-success";
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
