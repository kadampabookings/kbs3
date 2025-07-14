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
import one.modality.event.frontoffice.activities.booking.process.event.bookingform.multipages.BookingFormPage;
import one.modality.event.frontoffice.activities.booking.process.event.bookingform.util.BookingFormUtil;
import org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.OnlineFestivalI18nKeys;

import java.util.List;

/**
 * @author Bruno Salmon
 */
public final class TeachingPage implements BookingFormPage {

    private final GridPane gridPane = BookingFormUtil.createOptionsGridPane(true);
    private final Label bottomLabel = BookingFormUtil.createStrongLabel();
    private final VBox container = BookingFormUtil.createPageVBox("teaching-options", true,
        BookingFormUtil.createSecondaryLabel(OnlineFestivalI18nKeys.BookingOptions),
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
            TeachingPeriodOption teachingPeriodOptionView = new TeachingPeriodOption(bookablePeriod, workingBookingProperties.getWorkingBooking());
            RadioButton radioButton = teachingPeriodOptionView.getRadioButton();
            radioButton.setToggleGroup(teachingOptionsToggleGroup);
            gridPane.add(radioButton, 0, i);
            gridPane.add(teachingPeriodOptionView.getPeriodLabel(), 1, i);
            gridPane.add(teachingPeriodOptionView.getPriceLabel(), 2, i);
            if (n == 1)
                radioButton.setSelected(true);
        }
        I18nControls.bindI18nProperties(bottomLabel, OnlineFestivalI18nKeys.OnlineFestivalTeachingBottomMessage1, LocalizedTime.formatLocalDateTimeProperty(policyAggregate.getEvent().getVodExpirationDate(), FrontOfficeTimeFormats.MEDIA_EXPIRATION_DATE_TIME_FORMAT));
    }

}
