package org.kadampabookings.kbs.frontoffice.bookingform.mkmc.inpersonretreat.sections;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import one.modality.booking.client.workingbooking.WorkingBookingProperties;
import one.modality.booking.frontoffice.bookingpage.BookingFormSection;

/**
 * Section for audio recordings selection - using CSS classes for all styling
 *
 * @author Bruno Salmon
 */
public class AudioRecordingsSection implements BookingFormSection {

    private final VBox container = new VBox(16);

    public AudioRecordingsSection() {
        container.setPadding(new Insets(0, 0, 40, 0));

        // Section title wrapped in container for proper border styling
        VBox titleContainer = new VBox();
        titleContainer.getStyleClass().add("section-title-container");
        titleContainer.setPadding(new Insets(14, 18, 14, 18));

        Label sectionTitle = new Label("Audio recordings");
        sectionTitle.getStyleClass().add("section-title-text");
        titleContainer.getChildren().add(sectionTitle);

        Label noteText = new Label(
                "Audio recordings are available of the teaching sessions only. " +
                "Note recordings do not include the empowerments, guided meditation or retreat sessions.");
        noteText.getStyleClass().add("note-text");
        noteText.setWrapText(true);

        // Radio options
        HBox radioGroup = new HBox(8);
        radioGroup.setPadding(new Insets(16, 0, 0, 0));
        ToggleGroup audioToggle = new ToggleGroup();

        ToggleButton noAudioBtn = createInlineRadioButton("No audio recordings", audioToggle, true);
        ToggleButton audioBtn = createInlineRadioButton("Audio recordings in mp3 format - £15", audioToggle, false);

        radioGroup.getChildren().addAll(noAudioBtn, audioBtn);

        container.getChildren().addAll(titleContainer, noteText, radioGroup);
    }

    private ToggleButton createInlineRadioButton(String text, ToggleGroup group, boolean selected) {
        ToggleButton btn = new ToggleButton(text);
        btn.setToggleGroup(group);
        btn.setSelected(selected);
        btn.getStyleClass().add("inline-radio-button");
        if (selected) {
            btn.getStyleClass().add("selected");
        }
        btn.setPadding(new Insets(10, 18, 10, 18));

        btn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                btn.getStyleClass().add("selected");
            } else {
                btn.getStyleClass().remove("selected");
            }
        });

        return btn;
    }

    @Override
    public Object getTitleI18nKey() {
        return "Audio recordings";
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
