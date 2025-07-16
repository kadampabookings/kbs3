package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.teaching;

import dev.webfx.extras.i18n.controls.I18nControls;
import dev.webfx.extras.time.format.LocalizedTime;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import one.modality.base.client.time.FrontOfficeTimeFormats;
import one.modality.base.shared.entities.BookablePeriod;
import one.modality.base.shared.knownitems.KnownItemI18nKeys;
import one.modality.ecommerce.client.workingbooking.WorkingBookingProperties;
import one.modality.ecommerce.document.service.PolicyAggregate;
import one.modality.ecommerce.frontoffice.bookingform.BookingFormI18nKeys;
import one.modality.ecommerce.frontoffice.bookingform.multipages.BookingFormPage;
import one.modality.ecommerce.frontoffice.bookingelements.BookingElements;
import org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.OnlineFestivalI18nKeys;

import java.util.List;

/**
 * @author Bruno Salmon
 */
public final class TeachingPage implements BookingFormPage {

    private final GridPane gridPane = BookingElements.createOptionsGridPane(true);
    private final Label bottomLabel = BookingElements.createStrongLabel();
    private final VBox container = BookingElements.createPageVBox("teaching-options", true,
        BookingElements.createSecondaryLabel(BookingFormI18nKeys.BookingOptions),
        gridPane,
        bottomLabel
    );

    @Override
    public Object getTitleI18nKey() {
        return KnownItemI18nKeys.TeachingsOnline;
    }

    @Override
    public Node getView() {
        return container;
    }

    @Override
    public void setWorkingBookingProperties(WorkingBookingProperties workingBookingProperties) {
        gridPane.getChildren().clear();
        PolicyAggregate policyAggregate = workingBookingProperties.getPolicyAggregate();
        List<BookablePeriod> bookablePeriods = policyAggregate.getBookablePeriods();
        ToggleGroup teachingOptionsToggleGroup = new ToggleGroup();
        for (int i = 0, n = bookablePeriods.size(); i < n; i++) {
            BookablePeriod bookablePeriod = bookablePeriods.get(i);
            TeachingPeriodOption teachingPeriodOption = new TeachingPeriodOption(bookablePeriod, workingBookingProperties.getWorkingBooking());
            RadioButton radioButton = teachingPeriodOption.getRadioButton();
            radioButton.setToggleGroup(teachingOptionsToggleGroup);
            gridPane.add(radioButton, 0, i);
            gridPane.add(teachingPeriodOption.getPeriodLabel(), 1, i);
            gridPane.add(teachingPeriodOption.getPriceLabel(), 2, i);
            if (n == 1)
                radioButton.setSelected(true);
        }
        I18nControls.bindI18nProperties(bottomLabel, OnlineFestivalI18nKeys.OnlineFestivalTeachingBottomMessage1, LocalizedTime.formatLocalDateTimeProperty(policyAggregate.getEvent().getVodExpirationDate(), FrontOfficeTimeFormats.MEDIA_EXPIRATION_DATE_TIME_FORMAT));
    }

}
