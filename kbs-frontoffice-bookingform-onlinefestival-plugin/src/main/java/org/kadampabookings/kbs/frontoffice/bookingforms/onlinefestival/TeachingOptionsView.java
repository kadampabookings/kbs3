package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import one.modality.base.shared.entities.BookablePeriod;
import one.modality.ecommerce.client.workingbooking.WorkingBooking;
import one.modality.ecommerce.document.service.PolicyAggregate;

import java.util.List;

/**
 * @author Bruno Salmon
 */
final class TeachingOptionsView {

    private final GridPane teachingOptionsGridPane = new GridPane(20, 20);

    public TeachingOptionsView(WorkingBooking workingBooking) {
        PolicyAggregate policyAggregate = workingBooking.getPolicyAggregate();
        List<BookablePeriod> bookablePeriods = policyAggregate.getBookablePeriods();
        ToggleGroup teachingOptionsToggleGroup = new ToggleGroup();
        for (int i = 0, n = bookablePeriods.size(); i < n; i++) {
            BookablePeriod bookablePeriod = bookablePeriods.get(i);
            TeachingOptionView teachingOptionView = new TeachingOptionView(bookablePeriod, workingBooking);
            RadioButton radioButton = teachingOptionView.getRadioButton();
            radioButton.setToggleGroup(teachingOptionsToggleGroup);
            teachingOptionsGridPane.add(radioButton, 0, i);
            teachingOptionsGridPane.add(teachingOptionView.getPeriodLabel(), 1, i);
            teachingOptionsGridPane.add(teachingOptionView.getPriceLabel(), 2, i);
            if (n == 1)
                radioButton.setSelected(true);
        }
        teachingOptionsGridPane.setAlignment(Pos.CENTER);
        teachingOptionsGridPane.getStyleClass().addAll("booking-options", "teaching-options");
        BorderPane.setMargin(teachingOptionsGridPane, new Insets(48, 0, 48, 0));
    }

    Node getView() {
        return teachingOptionsGridPane;
    }
}
