package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.audiorecording;

import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import one.modality.base.client.i18n.I18nEntities;
import one.modality.base.client.time.ModalityDates;
import one.modality.base.shared.entities.Item;
import one.modality.base.shared.entities.ScheduledItem;
import one.modality.booking.client.workingbooking.WorkingBooking;
import one.modality.booking.frontoffice.bookingelements.BookingElements;

import java.util.List;

/**
 * @author Bruno Salmon
 */
final class AudioRecordingLanguageAndPeriodOption {

    private final CheckBox bookButton = BookingElements.optionLabel(new CheckBox());
    private final Label periodLabel = BookingElements.createPeriodLabel();
    private final Label priceLabel = BookingElements.createPriceLabel();

    AudioRecordingLanguageAndPeriodOption(Item item, List<ScheduledItem> scheduledItems, WorkingBooking workingBooking) {
        I18nEntities.bindExpressionTextProperty(bookButton, item, "i18n(this)");
        periodLabel.setText(ModalityDates.formatHasDateSeries(scheduledItems));
        BookingElements.setupPeriodOption(scheduledItems, priceLabel, bookButton.selectedProperty(), workingBooking);
    }

    ButtonBase getBookButton() {
        return bookButton;
    }

    Label getPeriodLabel() {
        return periodLabel;
    }

    Label getPriceLabel() {
        return priceLabel;
    }

}
