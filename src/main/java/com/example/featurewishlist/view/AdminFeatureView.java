package com.example.featurewishlist.view;

import com.example.featurewishlist.model.FeatureRequest;
import com.example.featurewishlist.model.FeatureStatus;
import com.example.featurewishlist.repository.FeatureRequestRepository;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.StringLengthValidator;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

import java.net.URI;
import java.util.List;

@Route("admin")
@PageTitle("Admin – Features bearbeiten")
@RolesAllowed("ADMIN")
public class AdminFeatureView extends VerticalLayout {

    private final FeatureRequestRepository repository;
    private final Grid<FeatureRequest> grid = new Grid<>(FeatureRequest.class, false);

    public AdminFeatureView(FeatureRequestRepository repository) {
        this.repository = repository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new H3("Admin: Features verwalten"));
        configureGrid();
        reload();
    }

    private void configureGrid() {
        grid.removeAllColumns(); // Safety: alte Spalten weg
        grid.setWidthFull();
        grid.setAllRowsVisible(true);

        // Button-Spalte ganz nach vorne und fixiert
        grid.addComponentColumn(fr -> new Button("Bearbeiten", e -> openEditDialog(fr)))
            .setHeader("Aktion")
            .setKey("edit")
            .setAutoWidth(true)
            .setFrozen(true)       // << fix
            .setFlexGrow(0);

        grid.addColumn(FeatureRequest::getTitle)
            .setHeader("Titel")
            .setSortable(true)
            .setAutoWidth(true)
            .setFlexGrow(2);

        grid.addColumn(FeatureRequest::getCategory)
            .setHeader("Kategorie")
            .setSortable(true)
            .setAutoWidth(true);

        grid.addColumn(FeatureRequest::getStatus)
            .setHeader("Status")
            .setSortable(true)
            .setAutoWidth(true);

        grid.addColumn(FeatureRequest::getTicketUrl)
            .setHeader("Ticket-URL")
            .setAutoWidth(true);

        grid.addColumn(FeatureRequest::getCreatedAt)
            .setHeader("Erstellt am")
            .setSortable(true)
            .setAutoWidth(true);

        add(grid);
    }


    private Button createEditButton(FeatureRequest fr) {
        return new Button("Bearbeiten", e -> openEditDialog(fr));
    }

    private void openEditDialog(FeatureRequest fr) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Feature bearbeiten");

        // Felder
        TextField title = new TextField("Titel");
        TextField category = new TextField("Kategorie");

        Select<FeatureStatus> status = new Select<>(); // wichtig: leerer Ctor
        status.setItems(FeatureStatus.values());
        status.setLabel("Status");

        TextField ticketUrl = new TextField("Ticket-URL (optional)");
        ticketUrl.setPlaceholder("https://jira/... ODER Ticket-Key (z. B. PROJ-123)");
        ticketUrl.setClearButtonVisible(true);
        ticketUrl.setWidthFull();

        TextArea description = new TextArea("Beschreibung");
        description.setWidthFull();
        description.setMinHeight("8rem");

        // Binder + Validierung
        Binder<FeatureRequest> binder = new Binder<>(FeatureRequest.class);

        binder.forField(title)
            .asRequired("Titel ist erforderlich")
            .withValidator(new StringLengthValidator("Mind. 3 Zeichen", 3, 255))
            .bind(FeatureRequest::getTitle, FeatureRequest::setTitle);

        binder.forField(category)
            .withValidator(v -> v == null || v.length() <= 255, "Max. 255 Zeichen")
            .bind(FeatureRequest::getCategory, FeatureRequest::setCategory);

        binder.forField(status)
            .asRequired("Status ist erforderlich")
            .bind(FeatureRequest::getStatus, FeatureRequest::setStatus);

        // Ticket-URL: leer ODER http/https ODER Ticket-Key (wird bei Save zur URL gebaut, wenn Basis gesetzt)
        binder.forField(ticketUrl)
            .withValidator(v -> v == null || v.isBlank() || looksLikeUrl(v) || looksLikeTicketKey(v),
                "Gültige URL (http/https) oder Ticket-Key wie PROJ-123")
            .bind(FeatureRequest::getTicketUrl, FeatureRequest::setTicketUrl);

        binder.forField(description)
            .withValidator(v -> v == null || v.length() <= 5000, "Max. 5000 Zeichen")
            .bind(FeatureRequest::getDescription, FeatureRequest::setDescription);

        binder.readBean(fr);

        // Layout
        FormLayout form = new FormLayout(title, category, status, ticketUrl, description);
        form.setWidth("900px");
        form.setColspan(description, 2);

        Button save = new Button("Speichern", e -> {
            // Validierung
            var result = binder.validate();
            if (!result.isOk()) {
                Notification.show("Bitte Eingaben prüfen.", 3000, Notification.Position.MIDDLE);
                return;
            }

            // Ticket-Key → URL bauen, falls Basis vorhanden (12‑Factor via ENV)
            String base = System.getenv("APP_TICKET_BASE_URL"); // z. B. https://jira.example.com/browse/
            String val = ticketUrl.getValue();
            if (val != null && !val.isBlank() && !looksLikeUrl(val) && looksLikeTicketKey(val) && base != null && !base.isBlank()) {
                ticketUrl.setValue(base.endsWith("/") ? base + val : base + "/" + val);
            }

            try {
                if (binder.writeBeanIfValid(fr)) {
                    repository.save(fr);
                    Notification.show("Gespeichert");
                    dialog.close();
                    reload();
                }
            } catch (Exception ex) {
                Notification.show("Fehler beim Speichern: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
            }
        });
        Button cancel = new Button("Abbrechen", e -> dialog.close());

        HorizontalLayout buttons = new HorizontalLayout(save, cancel);
        buttons.setSpacing(true);

        dialog.add(form, buttons);
        dialog.setModal(true);
        dialog.setDraggable(true);
        dialog.setResizable(true);
        dialog.open();
    }

    private void reload() {
        List<FeatureRequest> items = repository.findAll();
        grid.setItems(items);
    }

    // --- kleine Helfer --- //
    private boolean looksLikeUrl(String v) {
        try {
            var u = new URI(v);
            var s = u.getScheme();
            return s != null && (s.equalsIgnoreCase("http") || s.equalsIgnoreCase("https"));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean looksLikeTicketKey(String v) {
        // einfache Heuristik: ABC-123
        return v != null && v.matches("[A-Z][A-Z0-9]+-\\d+");
    }
}
