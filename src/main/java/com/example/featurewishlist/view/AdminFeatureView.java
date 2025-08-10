package com.example.featurewishlist.view;

import com.example.featurewishlist.model.FeatureRequest;
import com.example.featurewishlist.model.FeatureStatus;
import com.example.featurewishlist.repository.FeatureRequestRepository;
import com.example.featurewishlist.repository.VoteRepository;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.SortDirection;
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
    private final VoteRepository voteRepository;
    private final Grid<FeatureRequest> grid = new Grid<>(FeatureRequest.class, false);

    public AdminFeatureView(FeatureRequestRepository repository, VoteRepository voteRepository) {
        this.repository = repository;
        this.voteRepository = voteRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new H3("Admin: Features verwalten"));
        configureGrid();
        reload();
    }

    private void configureGrid() {
        grid.removeAllColumns();
        grid.setWidthFull();
        grid.setAllRowsVisible(true);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_WRAP_CELL_CONTENT);

        // Edit-Button pro Zeile (links fixiert)
        grid.addComponentColumn(fr -> {
            Button edit = new Button(new Icon(VaadinIcon.EDIT));
            edit.getElement().setAttribute("title", "Feature bearbeiten");
            edit.addClickListener(e -> openEditDialog(fr));
            return edit;
        }).setHeader("Aktion").setFrozen(true).setFlexGrow(0).setAutoWidth(true);

        grid.addColumn(FeatureRequest::getTitle)
            .setHeader("Titel").setSortable(true).setAutoWidth(true).setFlexGrow(2);

        grid.addColumn(FeatureRequest::getCategory)
            .setHeader("Kategorie").setSortable(true).setAutoWidth(true);

        grid.addColumn(FeatureRequest::getStatus)
            .setHeader("Status").setSortable(true).setAutoWidth(true);

        // Votes-Spalte
        grid.addColumn(fr -> voteRepository.countByFeature(fr))
            .setHeader("Votes")
            .setKey("votes")
            .setComparator((a, b) -> Long.compare(
                voteRepository.countByFeature(a), voteRepository.countByFeature(b)))
            .setAutoWidth(true);

        grid.addColumn(FeatureRequest::getTicketUrl)
            .setHeader("Ticket-URL").setAutoWidth(true);

        grid.addColumn(FeatureRequest::getCreatedAt)
            .setHeader("Erstellt am").setSortable(true).setAutoWidth(true);

        // Zeilen-Highlight für offene Features
        grid.setClassNameGenerator(fr -> fr.getStatus() == FeatureStatus.OPEN ? "status-open" : "");

        add(grid);

        // CSS für linke farbige Kante bei offenen Features
        getElement().executeJs("""
            const s=document.createElement('style');
            s.innerHTML='.status-open>td:first-child{border-left:3px solid var(--lumo-primary-color)}';
            document.head.appendChild(s);
        """);
    }

    private void openEditDialog(FeatureRequest fr) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Feature bearbeiten");
        dialog.setModal(true);
        dialog.setDraggable(true);
        dialog.setResizable(true);
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(false);

        TextField title = new TextField("Titel");
        TextField category = new TextField("Kategorie");

        Select<FeatureStatus> status = new Select<>();
        status.setItems(FeatureStatus.values());
        status.setLabel("Status");

        TextField ticketUrl = new TextField("Ticket-URL (optional)");
        ticketUrl.setPlaceholder("https://jira/... ODER Ticket-Key");
        ticketUrl.setClearButtonVisible(true);
        ticketUrl.setWidthFull();

        TextArea description = new TextArea("Beschreibung");
        description.setWidthFull();
        description.setMinHeight("8rem");

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

        binder.forField(ticketUrl)
            .withValidator(v -> v == null || v.isBlank() || looksLikeUrl(v) || looksLikeTicketKey(v),
                "Gültige URL oder Ticket-Key wie PROJ-123")
            .bind(FeatureRequest::getTicketUrl, FeatureRequest::setTicketUrl);

        binder.forField(description)
            .withValidator(v -> v == null || v.length() <= 5000, "Max. 5000 Zeichen")
            .bind(FeatureRequest::getDescription, FeatureRequest::setDescription);

        binder.readBean(fr);
        status.setValue(fr.getStatus());

        FormLayout form = new FormLayout(title, category, status, ticketUrl, description);
        form.setWidth("900px");
        form.setColspan(description, 2);

        Button save = new Button("Speichern", new Icon(VaadinIcon.CHECK));
        save.getElement().getThemeList().add("primary");
        save.setEnabled(false);
        save.addClickShortcut(Key.ENTER);

        Button cancel = new Button("Abbrechen", new Icon(VaadinIcon.CLOSE_SMALL));
        cancel.addClickShortcut(Key.ESCAPE);
        cancel.addClickListener(e -> dialog.close());

        binder.addStatusChangeListener(ev -> {
            boolean valid = !ev.hasValidationErrors();
            boolean dirty = binder.hasChanges();
            save.setEnabled(valid && dirty);
        });

        save.addClickListener(e -> {
            String val = ticketUrl.getValue();
            String base = System.getenv("APP_TICKET_BASE_URL");
            if (val != null && !val.isBlank() && !looksLikeUrl(val) && looksLikeTicketKey(val)
                    && base != null && !base.isBlank()) {
                ticketUrl.setValue(base.endsWith("/") ? base + val : base + "/" + val);
            }
            if (binder.writeBeanIfValid(fr)) {
                repository.save(fr);
                Notification.show("Gespeichert");
                dialog.close();
                reload();
            } else {
                Notification.show("Bitte Eingaben prüfen.", 3000, Notification.Position.MIDDLE);
            }
        });

        HorizontalLayout buttons = new HorizontalLayout(save, cancel);
        dialog.add(form, buttons);
        dialog.open();
    }

    private void reload() {
        List<FeatureRequest> items = repository.findAll();
        grid.setItems(items);

        var votesCol = grid.getColumnByKey("votes");
        if (votesCol != null) {
            grid.sort(List.of(new GridSortOrder<>(votesCol, SortDirection.DESCENDING)));
        }
    }

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
        return v != null && v.matches("[A-Z][A-Z0-9]+-\\d+");
    }
}
