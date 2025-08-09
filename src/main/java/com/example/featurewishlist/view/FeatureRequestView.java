package com.example.featurewishlist.view;

import com.example.featurewishlist.model.FeatureRequest;
import com.example.featurewishlist.model.FeatureStatus;
import com.example.featurewishlist.repository.FeatureRequestRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

@Route("add")
@PageTitle("Feature-Wunsch einreichen")
public class FeatureRequestView extends VerticalLayout {

    public FeatureRequestView(@Autowired FeatureRequestRepository repository) {
        TextField title = new TextField("Titel");
        TextArea description = new TextArea("Beschreibung");
        TextField category = new TextField("Kategorie");

        Button submit = new Button("Einreichen");
        submit.addClickListener(e -> {
            FeatureRequest request = FeatureRequest.builder()
                .title(title.getValue())
                .description(description.getValue())
                .category(category.getValue())
                .status(FeatureStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();
            repository.save(request);
            Notification.show("Feature gespeichert!");
            title.clear();
            description.clear();
            category.clear();
        });

        add(title, description, category, submit);
    }
}
