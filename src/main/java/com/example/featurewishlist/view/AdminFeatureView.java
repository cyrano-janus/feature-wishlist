package com.example.featurewishlist.view;

import com.example.featurewishlist.model.FeatureRequest;
import com.example.featurewishlist.model.FeatureStatus;
import com.example.featurewishlist.repository.FeatureRequestRepository;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
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
    private final Binder<FeatureRequest> binder = new Binder<>(FeatureRequest.class);
    private final Editor<FeatureRequest> editor = grid.getEditor();

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
        grid.setWidthFull();

        grid.addColumn(FeatureRequest::getTitle)
            .setHeader("Titel")
            .setAutoWidth(true)
            .setFlexGrow(1);

        grid.addColumn(FeatureRequest::getCategory)
            .setHeader("Kategorie")
            .setAutoWidth(true)
            .setFlexGrow(0);

        grid.addColumn(FeatureRequest::getCreatedAt)
            .setHeader("Erstellt am")
            .setAutoWidth(true)
            .setFlexGrow(0);

        // ---- Editor-Felder
        Select<FeatureStatus> statusEditor = new Select<>();   // <- FIX: leerer Ctor
        statusEditor.setItems(FeatureStatus.values());         // <- Items separat setzen
        statusEditor.setWidth("180px");

        TextField ticketEditor = new TextField();
        ticketEditor.setPlaceholder("https://jira/... (optional)");
        ticketEditor.setWidth("320px");

        // Validierung: optional, aber wenn gesetzt → gültige http/https-URL
        binder.forField(ticketEditor)
            .withValidator(
                value -> value == null || value.isBlank() || isValidUrl(value),
                "Bitte gültige URL (http/https) angeben oder leer lassen"
            )
            .bind(FeatureRequest::getTicketUrl, FeatureRequest::setTicketUrl);

        // Status-Bindung
        binder.forField(statusEditor)
            .asRequired("Status ist erforderlich")
            .bind(FeatureRequest::getStatus, FeatureRequest::setStatus);

        // Read-only Spalten + Aktionen
        grid.addColumn(FeatureRequest::getStatus).setHeader("Status (aktuell)");

        grid.addComponentColumn(fr -> {
            Button edit = new Button("Bearbeiten", e -> {
                if (editor.isOpen()) {
                    editor.cancel();
                }
                binder.readBean(fr);
                editor.editItem(fr);
            });
            return edit;
        }).setHeader("Aktion").setFlexGrow(0);

        grid.addComponentColumn(item -> {
            HorizontalLayout editorRow = new HorizontalLayout(statusEditor, ticketEditor);
            editorRow.setPadding(false);
            return editorRow;
        }).setHeader("Editor (Status + Ticket)").setFlexGrow(2);

        Button save = new Button("Speichern", e -> {
            FeatureRequest item = editor.getItem();
            if (item == null) return;
            try {
                if (binder.writeBeanIfValid(item)) {
                    repository.save(item);
                    Notification.show("Gespeichert");
                    editor.closeEditor();
                    reload();
                }
            } catch (Exception ex) {
                Notification.show("Fehler beim Speichern: " + ex.getMessage(),
                        4000, Notification.Position.MIDDLE);
            }
        });

        Button cancel = new Button("Abbrechen", e -> editor.cancel());

        grid.addComponentColumn(item -> new HorizontalLayout(save, cancel))
            .setHeader("Speichern")
            .setFlexGrow(0);

        editor.setBinder(binder);
        editor.setBuffered(true);

        add(grid);
    }

    private void reload() {
        List<FeatureRequest> items = repository.findAll();
        grid.setItems(items);
    }

    private boolean isValidUrl(String value) {
        try {
            URI u = new URI(value);
            String scheme = u.getScheme();
            return scheme != null && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"));
        } catch (Exception e) {
            return false;
        }
    }
}
