package org.kadampabookings.kbs.frontoffice.bookingform.mkmc.inpersonretreat.sections;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import one.modality.booking.client.workingbooking.WorkingBookingProperties;
import one.modality.booking.frontoffice.bookingpage.BookingFormSection;

/**
 * Rate type selection section - using CSS classes for all styling
 *
 * @author Bruno Salmon
 */
public class RateTypeSection implements BookingFormSection {

    private final VBox container = new VBox(16);

    public RateTypeSection() {
        container.setPadding(new Insets(0, 0, 40, 0));

        Label label = new Label("Rate Type"); // TODO: I18n
        label.getStyleClass().add("form-label-text");

        HBox rateOptions = new HBox(16);
        ToggleGroup group = new ToggleGroup();

        rateOptions.getChildren().addAll(
                createRateOption("Standard Rate", group, true),
                createRateOption("Reduced Rate (Low Income)", group, false),
                createRateOption("Member Rate", group, false));

        container.getChildren().addAll(label, rateOptions);
    }

    private Node createRateOption(String text, ToggleGroup group, boolean selected) {
        HBox option = new HBox(10);
        option.setPadding(new Insets(14, 24, 14, 24));
        option.getStyleClass().add("rate-option");
        if (selected) {
            option.getStyleClass().add("selected");
        }
        HBox.setHgrow(option, Priority.ALWAYS);
        option.setMinWidth(200);

        RadioButton rb = new RadioButton(text);
        rb.setToggleGroup(group);
        rb.setSelected(selected);

        option.getChildren().add(rb);

        // Selection styling logic using CSS classes
        rb.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                option.getStyleClass().add("selected");
            } else {
                option.getStyleClass().remove("selected");
            }
        });

        // Make the whole box clickable
        option.setOnMouseClicked(e -> rb.setSelected(true));

        return option;
    }

    @Override
    public Object getTitleI18nKey() {
        return "Rate Type"; // TODO: Use I18n key
    }

    @Override
    public Node getView() {
        return container;
    }

    @Override
    public void setWorkingBookingProperties(WorkingBookingProperties workingBookingProperties) {
        // TODO: Bind to actual working booking data
    }
}
