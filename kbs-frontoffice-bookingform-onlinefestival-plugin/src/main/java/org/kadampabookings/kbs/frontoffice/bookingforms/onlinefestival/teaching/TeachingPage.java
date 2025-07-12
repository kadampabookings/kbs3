package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.teaching;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import one.modality.base.shared.entities.BookablePeriod;
import one.modality.base.shared.knownitems.KnownItemI18nKeys;
import one.modality.ecommerce.client.workingbooking.WorkingBookingProperties;
import one.modality.ecommerce.document.service.PolicyAggregate;
import one.modality.event.frontoffice.activities.booking.process.event.bookingform.multipages.BookingFormPage;

import java.util.List;

/**
 * @author Bruno Salmon
 */
public final class TeachingPage implements BookingFormPage {

    private final GridPane gridPane = new GridPane(20, 20);

    public TeachingPage() {
        gridPane.setAlignment(Pos.CENTER);
        gridPane.getStyleClass().addAll("booking-options", "teaching-options");
        gridPane.setPadding(new Insets(48, 0, 48, 0));
    }

    @Override
    public Object getTitleI18nKey() {
        return KnownItemI18nKeys.TeachingsOnline;
    }

    @Override
    public Node getView() {
        return gridPane;
    }

    @Override
    public void setWorkingBookingProperties(WorkingBookingProperties workingBookingProperties) {
        gridPane.getChildren().clear();
        PolicyAggregate policyAggregate = workingBookingProperties.getPolicyAggregate();
        List<BookablePeriod> bookablePeriods = policyAggregate.getBookablePeriods();
        ToggleGroup teachingOptionsToggleGroup = new ToggleGroup();
        for (int i = 0, n = bookablePeriods.size(); i < n; i++) {
            BookablePeriod bookablePeriod = bookablePeriods.get(i);
            TeachingPeriodOption teachingPeriodOptionView = new TeachingPeriodOption(bookablePeriod, workingBookingProperties.getWorkingBooking());
            RadioButton radioButton = teachingPeriodOptionView.getRadioButton();
            radioButton.setToggleGroup(teachingOptionsToggleGroup);
            gridPane.add(radioButton, 0, i);
            gridPane.add(teachingPeriodOptionView.getPeriodLabel(), 1, i);
            gridPane.add(teachingPeriodOptionView.getPriceLabel(), 2, i);
            if (n == 1)
                radioButton.setSelected(true);
        }
    }

}
